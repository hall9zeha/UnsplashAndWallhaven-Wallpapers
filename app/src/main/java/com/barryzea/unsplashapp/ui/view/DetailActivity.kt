package com.barryzea.unsplashapp.ui.view

import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Color

import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import com.barryzea.unsplashapp.MyApp
import com.barryzea.unsplashapp.R
import com.barryzea.unsplashapp.databinding.ActivityDetailBinding

import com.barryzea.unsplashapp.model.*
import com.barryzea.unsplashapp.model.database.Image
import com.barryzea.unsplashapp.ui.common.Constants
import com.barryzea.unsplashapp.ui.common.ImageHelper
import com.barryzea.unsplashapp.ui.common.PermissionRequester
import com.barryzea.unsplashapp.ui.common.loadUrl
import com.barryzea.unsplashapp.ui.view.dialog.SetWallpaperFragment
import com.barryzea.unsplashapp.viewModel.ViewModelMain
import com.barryzea.unsplashapp.viewModel.ViewModelSearch
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.theartofdev.edmodo.cropper.CropImage
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.steamcrafted.loadtoast.LoadToast
import xyz.belvi.blurhash.BlurHashDecoder
import java.lang.Exception

@AndroidEntryPoint
class DetailActivity : AppCompatActivity() {
    private lateinit var bind: ActivityDetailBinding
    private  var unsplashResult:UnsplashResult?=null
    private var wallHavenResult:WallHavenImageDetail?=null
    private var imageExtra:Image?=null
    private var image= Image()
    private var imageFavFound:Image?=null
    private var isPressed=true
    private var isExpanded=false
    private var isFavorite=false
    private lateinit var bottomSheetBehavior:BottomSheetBehavior<View>
    private val permissionRequest=PermissionRequester(this, WRITE_EXTERNAL_STORAGE)
    private val prefDataSourceOrigin:Int by lazy {MyApp.prefs.dataSourceOrigin}
    private val viewModel:ViewModelSearch by viewModels()
    private val viewModelMain:ViewModelMain by viewModels()

    lateinit var loadToast:LoadToast
    val resultLauncher=registerForActivityResult(ActivityResultContracts.StartActivityForResult()){ it ->
         if(it.resultCode==Activity.RESULT_OK){
            val result=CropImage.getActivityResult(it.data)
            val uriCropped=result.uri
            val bitmap:Bitmap=MediaStore.Images.Media.getBitmap(contentResolver,uriCropped)
            //setear el fondo de pantalla aquí
             bitmap?.let{ bitmap->
                SetWallpaperFragment(this,bitmap).show(supportFragmentManager.beginTransaction(),SetWallpaperFragment::class.simpleName)
             }
            //*******************************
        }
        else{
            loadToast?.hide()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        bind= ActivityDetailBinding.inflate(layoutInflater)
        setContentView(bind.root)
        getIntentExtras()
        setUpLoadToast()
        populateDetailImage()

        setUpListenerButtons()
        setUpBottomSheetDetail()
        contractAndExpandPhoto()
        viewModel.getWHImageDetail().observe(this,Observer(::updateUiWallhavenImageDetail))
        viewModel.getDownloadLocation().observe(this,Observer(::observeDownloadLocation))
        viewModel.getImageDetailUnsplash().observe(this,Observer(::updateUiImageDetailUnsplash))

    }

    private fun updateUiImageDetailUnsplash(unsplashResult: UnsplashResult?) = with(bind) {
        var bitmap:Bitmap?=null
        this@DetailActivity.unsplashResult= unsplashResult
        unsplashResult?.let { res ->
            if (res.errorCatch == 0) {
                hideLoadingView(true)
                photoView.loadUrl(bind.root, res.urls.full!!)
                detailLayout.chipNameUser.text = res.user.username
                detailLayout.chipCreateAt.text = res.createdAt?.substring(0, 10)
                detailLayout.chipLikes.text = res.likes.toString()
                detailLayout.chipSizeImage.text = String.format("%s x %s", res.width, res.height)
                detailLayout.chipTotalPhotos.text = res.user.totalPhotos
                detailLayout.chipLinkUser.setOnClickListener { openUrlView(res.user.links.html.toString()) }
                detailLayout.chipNameUser.setOnClickListener { openUrlView(res.user.links.html.toString()) }
                detailLayout.btnColor.backgroundTintList =
                    ColorStateList.valueOf(Color.parseColor(res.color!!))
                lifecycleScope.launch(Dispatchers.IO) {
                    bitmap = BlurHashDecoder.decode(res.blurHash.toString(), 20, 18)

                    withContext(Dispatchers.Main) {
                        photoView.setImageBitmap(bitmap)
                        photoView.setScale(3f, true)
                    }
                }
                Glide.with(this@DetailActivity)
                    .load(res.user.profileImg.medium)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .circleCrop()
                    .into(detailLayout.ivUser)

            }
            else{
                Toast.makeText(this@DetailActivity, getString(R.string.error_loading_detail), Toast.LENGTH_LONG).show()
            }
        }

    }

    private fun observeDownloadLocation(downloadLocation: DownloadLocation?) {
        downloadLocation?.let{
            ImageHelper.downloadImage(this,unsplashResult?.id.toString(),it.url.toString())
        }
    }
    private fun getIntentExtras(){
        intent?.let{

            imageExtra =   it.getParcelableExtra(Constants.IMAGE_FAV_EXTRA)
            imageExtra?.let{image->
                checkIfIsFavorite(image.idImage)
            }
        }
   }
    private fun checkIfIsFavorite(idImage:String){
         lifecycleScope.launch(Dispatchers.Main){

            imageFavFound=viewModelMain.getFavById(idImage)

            imageFavFound?.let{
                bind.btnFavorite.setIconResource(R.drawable.ic_favorite)
                isFavorite=true
            }

        }

    }
    private fun setUpLoadToast(){
        loadToast= LoadToast(this)
        loadToast.setText(getString(R.string.workingMsg))
        loadToast.setProgressColor(Color.CYAN)
        loadToast.setTranslationY(120)
    }
    private fun hideLoadingView(isVisible:Boolean){
        if(isVisible) {
            bind.clLoadingDetail.visibility=View.GONE
            bind.clPhotoView.visibility = View.VISIBLE
            bind.clBottomSheet.visibility = View.VISIBLE

        }
    }

    private fun populateDetailImage(){

            imageExtra?.let { result ->

                if(result.idOrigin==Constants.WALLHAVEN_ORIGIN) {
                    viewModel.callWHUserDetail(result.idImage)
                }
                else{
                    viewModel.callImageDetailUnsplash(result.idImage)
                }
            }


    }
    private fun updateUiWallhavenImageDetail(userDetail: WallHavenImageById?) =with(bind){
        userDetail?.let {res->
            if(res.error==0) {
                hideLoadingView(true)

                wallHavenResult = res.dataImageDetail
                photoView.loadUrl(bind.root, res.dataImageDetail.path.toString())


                detailLayout.chipCreateAt.text = res.dataImageDetail.created_at?.substring(0, 10)
                detailLayout.chipLikes.text = res.dataImageDetail.favorites
                detailLayout.chipSizeImage.text = res.dataImageDetail.resolution
                detailLayout.chipTotalPhotos.text = res.dataImageDetail.file_Type
                bind.chipFileSize.visibility = View.VISIBLE
                bind.chipFileSize.text = humanReadableByteCountBin(res.dataImageDetail.file_size)
                detailLayout.chipLinkUser.setOnClickListener { openUrlView(res.dataImageDetail.url.toString()) }
                //detailLayout.chipNameUser.setOnClickListener { openUrlView(res.user.links.html.toString()) }
                visibilityButtonsColor(View.VISIBLE)
                detailLayout.btnColor.backgroundTintList =
                    ColorStateList.valueOf(Color.parseColor(res.dataImageDetail.colors!![0].toString()))
                detailLayout.btnColorOne.backgroundTintList =
                    ColorStateList.valueOf(Color.parseColor(res.dataImageDetail.colors!![1].toString()))
                detailLayout.btnColorTwo.backgroundTintList =
                    ColorStateList.valueOf(Color.parseColor(res.dataImageDetail.colors!![2].toString()))
                detailLayout.btnColorThird.backgroundTintList =
                    ColorStateList.valueOf(Color.parseColor(res.dataImageDetail.colors!![3].toString()))
                detailLayout.btnColorFour.backgroundTintList =
                    ColorStateList.valueOf(Color.parseColor(res.dataImageDetail.colors!![4].toString()))

                detailLayout.chipNameUser.text = res.dataImageDetail.uploader.username
                Glide.with(this@DetailActivity)
                    .load(res.dataImageDetail.uploader.avatar.bigAvatar)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .circleCrop()
                    .into(detailLayout.ivUser)
            }
            else{
                Toast.makeText(this@DetailActivity, getString(R.string.error_loading_detail), Toast.LENGTH_LONG).show()
            }
        }
    }
    private fun humanReadableByteCountBin(bytes: Long) = when {
        bytes == Long.MIN_VALUE || bytes < 0 -> "N/A"
        bytes < 1024L -> "$bytes B"
        bytes <= 0xfffccccccccccccL shr 40 -> "%.1f KiB".format(bytes.toDouble() / (0x1 shl 10))
        bytes <= 0xfffccccccccccccL shr 30 -> "%.1f MiB".format(bytes.toDouble() / (0x1 shl 20))
        bytes <= 0xfffccccccccccccL shr 20 -> "%.1f GiB".format(bytes.toDouble() / (0x1 shl 30))
        bytes <= 0xfffccccccccccccL shr 10 -> "%.1f TiB".format(bytes.toDouble() / (0x1 shl 40))
        bytes <= 0xfffccccccccccccL -> "%.1f PiB".format((bytes shr 10).toDouble() / (0x1 shl 40))
        else -> "%.1f EiB".format((bytes shr 20).toDouble() / (0x1 shl 40))
    }
    private fun visibilityButtonsColor(visibility:Int){
        bind.detailLayout.btnColorOne.visibility=visibility
        bind.detailLayout.btnColorTwo.visibility=visibility
        bind.detailLayout.btnColorThird.visibility=visibility
        bind.detailLayout.btnColorFour.visibility=visibility
    }

    private fun setUpListenerButtons(){
        bind.detailLayout.btnDownload.setOnClickListener {
            permissionRequest.request {

                    if(prefDataSourceOrigin==Constants.WALLHAVEN_ORIGIN){
                        wallHavenResult?.let {wh->
                            ImageHelper.downloadImage(this, "", wh.path.toString())
                        }
                    }
                    else{
                        unsplashResult?.let{u->
                            val ixId=u.links.downloadLocation.toString().substring(u.links.downloadLocation.toString().lastIndexOf("="))
                            viewModel.callDownloadLocation(u.id,ixId)
                            //ImageHelper.downloadImage(this,u.id,u.links.downloadLocation.toString())
                        }
                    }

                }
        }
        bind.detailLayout.btnShare.setOnClickListener {
            permissionRequest.request {
                if(prefDataSourceOrigin==Constants.WALLHAVEN_ORIGIN){
                    wallHavenResult?.let{wh->
                        ImageHelper.shareImage(wh.path,this,"")
                    }
                }
                else {
                    unsplashResult?.let { u ->
                        ImageHelper.shareImage(u.urls.full, this, u.id)
                    }
                }
            }
        }
        bind.detailLayout.btnSetWallpaper.setOnClickListener {
            permissionRequest.request {
                if(prefDataSourceOrigin==Constants.WALLHAVEN_ORIGIN){
                    wallHavenResult?.let{wh->
                        ImageHelper.cutImageForSetWallpaper(this@DetailActivity, "",wh.path.toString())
                    }
                }
                else{
                unsplashResult?.let {u->
                    ImageHelper.cutImageForSetWallpaper(this@DetailActivity, u.id,u.urls.full.toString())
                }
                    }
            }
        }
    }
    private fun setUpBottomSheetDetail(){

        bottomSheetBehavior= BottomSheetBehavior.from(bind.detailLayout.clDetailMain)
        bottomSheetBehavior.state=BottomSheetBehavior.STATE_COLLAPSED
        bottomSheetBehavior.addBottomSheetCallback(object:BottomSheetBehavior.BottomSheetCallback(){
            override fun onStateChanged(bottomSheet: View, newState: Int) {
                when(newState){
                    BottomSheetBehavior.STATE_EXPANDED-> {isExpanded=true;bind.detailLayout.btnExpand.setIconResource(R.drawable.ic_down)}
                    BottomSheetBehavior.STATE_COLLAPSED->{isExpanded=false;bind.detailLayout.btnExpand.setIconResource(R.drawable.ic_up)}
                }
            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {

            }
        })


    }
    private fun openUrlView(url:String){
        val uri=Uri.parse(url)
        val intent=Intent(Intent.ACTION_VIEW,uri)
        startActivity(intent)
    }

    private fun contractAndExpandPhoto(){
        bind.btnZoom.setOnClickListener {

            if(isPressed){
                isPressed=false
                bind.btnZoom.setIconResource(R.drawable.ic_expand)
                bind.photoView.setScale(1f,true)
            }
            else{
                isPressed=true
                bind.btnZoom.setIconResource(R.drawable.ic_contract)
                bind.photoView.setScale(3f,true)
            }
        }
        bind.btnFavorite.setOnClickListener {
            isFavorite = if(isFavorite){
                bind.btnFavorite.setIconResource(R.drawable.ic_favorite_outline)
                deleteFavorite(imageExtra)
                false
            } else{
                bind.btnFavorite.setIconResource(R.drawable.ic_favorite)
                saveFavorite()
                true
            }

        }
        bind.detailLayout.btnExpand.setOnClickListener {
            if(isExpanded){
                bottomSheetBehavior.state=BottomSheetBehavior.STATE_COLLAPSED
                bind.detailLayout.btnExpand.setIconResource(R.drawable.ic_up)
            }
            else{
                bottomSheetBehavior.state=BottomSheetBehavior.STATE_EXPANDED
                bind.detailLayout.btnExpand.setIconResource(R.drawable.ic_down)
            }
        }
    }
    private fun saveFavorite(){
        wallHavenResult?.let{
            image.imageUrl=it.thumbs?.original.toString()
            image.dimenY=it.dimension_y
            image.dimenX=it.dimension_x
            image.idImage=it.id.toString()
            image.idOrigin=Constants.WALLHAVEN_ORIGIN
            try {
                viewModelMain.saveFav(image)
                Toast.makeText(this, getString(R.string.favorite_saved), Toast.LENGTH_SHORT).show()
            }
            catch(e:Exception){
                Log.e("INSERT_ERROR", e.message.toString())
            }
        }?:run{
            unsplashResult?.let{
                image.imageUrl=it.urls.thumb.toString()
                image.dimenY=it.height
                image.dimenX=it.width
                image.idImage=it.id.toString()
                image.idOrigin=Constants.UNSPLASH_ORIGIN
                try {
                    viewModelMain.saveFav(image)
                    Toast.makeText(this, getString(R.string.favorite_saved), Toast.LENGTH_SHORT).show()
                }
                catch(e:Exception){
                    Log.e("INSERT_ERROR", e.message.toString())
                }
            }
        }
    }
    private fun deleteFavorite(imageExtra:Image?){
        imageExtra?.let{image->
            FavoriteFragment.viewModel?.deleteFav(image) ?:run{
                imageFavFound?.let {image->
                    viewModelMain.deleteFav(image)

                }
            }
        }
    }
    fun hideBottomSheetBehavior(){
        bottomSheetBehavior.state=BottomSheetBehavior.STATE_COLLAPSED
    }

    override fun onBackPressed() {
        if(isExpanded){
            bottomSheetBehavior.state=BottomSheetBehavior.STATE_COLLAPSED
        }
        else {
            super.onBackPressed()
        }
    }
}