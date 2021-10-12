package com.bignerdbranch.android.photogalleryx

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.util.DisplayMetrics
import android.util.Log
import android.view.*
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.SearchView
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.work.*
import java.util.concurrent.TimeUnit

private const val ARG_URI = "photo_page_url"
private const val TAG = "PhotoGalleryFragment"
private const val ERROR_TAG = "Error_TAG"
private const val POLL_WORK = "POLL_WORK"

class PhotoGalleryFragment : VisibleFragment() {

    private var navController: NavController? = null
    private lateinit var photoRecyclerView: RecyclerView
    private lateinit var thumbnailDownloader: ThumbnailDownloader<PhotoHolder>
    private lateinit var photoGalleryFragmentContext: Context

    /**
     * Property used to skip some block of codes in onCreateOptionMenu
     */
    private var shouldSkip = false

    /**
     * This property will be used to show a progress bar while FlickrFetcher fetches the JSON date
     */
    private lateinit var progressBar: ProgressBar

    private val photoGalleryViewModel by lazy { ViewModelProvider(this).get(PhotoGalleryViewModel::class.java) }


    override fun onCreate(savedInstanceState: Bundle?) {
        //region ENABLE STRICTMODE
        //  StrictMode.enableDefaults()
        //endregion
        //region INITIALIZATION OF SOME IMPORTANT COMPONENTS
        super.onCreate(savedInstanceState)
        photoGalleryFragmentContext = requireContext()
        retainInstance =
            true//rather than retain instance, you should use ViewModel to retain the necessary properties
        setHasOptionsMenu(true)
        //endregion
        //region wire PhotoGalleryFragment Handler into ThumbnailDownloader
        val responseHandler: Handler = Handler(requireContext().mainLooper)
        thumbnailDownloader =
            ThumbnailDownloader(responseHandler) { photoHolder, bitmap ->
                val drawable = BitmapDrawable(resources, bitmap)
                photoHolder.bindDrawable(drawable)
            }
        //endregion
        //region attach observer to [lifecycle] and [viewLifecycleOwner]
        viewLifecycleOwnerLiveData.observe(this, {
            it?.lifecycle?.addObserver(thumbnailDownloader.viewLifecycleObserver)
        })
        lifecycle.addObserver(thumbnailDownloader.fragmentLifecycleObserver)
        //endregion

        /*   //region create a work request, add constraints, and schedule it for execution.
           val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
           val workRequest = OneTimeWorkRequest.Builder(PollWorker::class.java).setConstraints(constraints).build()
           WorkManager.getInstance().enqueue(workRequest)
           //endregion
   */
    }

    override fun onResume() {
        super.onResume()
        //region DYNAMICALLY CHANGE THE spanCount OF (PHOTOVIEW.LAYOUTMANAGER AS GRIDLAYOUTMANAGER)
        val displayMetrics: DisplayMetrics = requireContext().resources.displayMetrics
        val dpWidth = displayMetrics.widthPixels
        val scalingfactor = 200
        val spanCount = (dpWidth / scalingfactor)
        photoRecyclerView.layoutManager = GridLayoutManager(context, spanCount)
        //endregion
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_photo_gallery, container, false)
        photoRecyclerView = view.findViewById(R.id.photo_recycler_view)
        photoRecyclerView.layoutManager = GridLayoutManager(context, 3)
        progressBar = view.findViewById(R.id.progressBar)//initialize the progressBar
        // viewLifecycleOwner.lifecycle.addObserver(thumbnailDownloader.viewLifecycleObserver)
        return view
        //region Add a listener to the recycler view to check if it has reached the end of the view
        /* photoRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
             override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                 super.onScrolled(recyclerView, dx, dy)
                 if (!recyclerView.canScrollVertically(1) && dy > 0) {
                     Log.d("Recyclerx", "onScrollStateChanged triggered: Can no longer scroll DOWN")

                 } else if (!recyclerView.canScrollVertically(-1) && dy < 0) {
                     Log.d("Recyclerx", "onScrollStateChanged triggered: Can no longer scroll UP")
                 }
             }
         })*/
        //endregion
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        photoGalleryViewModel.galleryItemLiveData.observe(
            viewLifecycleOwner,
            Observer { galleryItems ->
                Log.d(TAG, "$galleryItems")
                photoRecyclerView.adapter = PhotoAdapter(galleryItems)
                //dismiss the progressBar after the items are downloaded
                progressBar.visibility = View.GONE
            })
        navController = Navigation.findNavController(view)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_photo_gallery, menu)


        if (!shouldSkip) {
            val searchItem: MenuItem = menu.findItem(R.id.menu_item_search)
            val searchView = searchItem.actionView as SearchView
            searchView.apply {
                setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextSubmit(queryText: String): Boolean {
                        Log.d(TAG, "QueryTextSubmit: $queryText")
                        progressBar.visibility = View.VISIBLE // make the progressBar visible
                        photoGalleryViewModel.fetchPhotos(queryText)
                        clearFocus()//dismiss the soft keyboard
                        return true
                    }


                    override fun onQueryTextChange(queryText: String): Boolean {
                        Log.d(TAG, "QueryTextChange: $queryText")
                        return false
                    }
                })
                setOnFocusChangeListener { v, hasFocus ->
                    //when the searhView is no more in focus, colapse it
                    if (!hasFocus) {
                        searchItem.collapseActionView()
                    }
                }
                setOnSearchClickListener {
                    //place a text from QueryPreferences when the when the searchView is clicked
                    searchView.setQuery(photoGalleryViewModel.searchTerm, false)
                }

            }
            shouldSkip = true
        }
        //region Check whether the worker is already running and, if so, to set the correct title text
        val toggleItem = menu.findItem(R.id.menu_item_toggle_polling)
        val isPolling = QueryPreferences.isPolling(requireContext())
        val toggleItemTitle = if (isPolling) {
            R.string.stop_polling
        } else {
            R.string.start_polling
        }
        toggleItem.setTitle(toggleItemTitle)
        //endregion
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_item_clear -> {
                photoGalleryViewModel.fetchPhotos("")
                true
            }
            R.id.menu_item_toggle_polling -> {
                val isPolling = QueryPreferences.isPolling(requireContext())
                if (isPolling) {
                    WorkManager.getInstance().cancelUniqueWork(POLL_WORK)
                    QueryPreferences.setPolling(requireContext(), false)
                    Toast.makeText(
                        requireContext(),
                        "Polling has been DE-ACTIVATED",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    //region create a work request, add constraints, and schedule it for execution.
                    val constraints = Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                    val periodicRequest = PeriodicWorkRequest
                        .Builder(PollWorker::class.java, 15, TimeUnit.MINUTES)
                        .setConstraints(constraints)
                        .build()
                    WorkManager.getInstance().enqueueUniquePeriodicWork(
                        POLL_WORK,
                        ExistingPeriodicWorkPolicy.KEEP,
                        periodicRequest
                    )
                    QueryPreferences.setPolling(requireContext(), true)
                    Toast.makeText(
                        requireContext(),
                        "Polling has been ACTIVATED",
                        Toast.LENGTH_LONG
                    ).show()
                }

                //endregion
                activity?.invalidateOptionsMenu()
                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     *  The ViewHolder class for the [photoRecyclerView] *
     */
    private inner class PhotoHolder(itemImageView: AppCompatImageView) :
        RecyclerView.ViewHolder(itemImageView), View.OnClickListener {
        val bindDrawable: (Drawable) -> Unit = itemImageView::setImageDrawable
        private lateinit var galleryItem: GalleryItem

        init {
            itemView.setOnClickListener(this)
        }

        fun bindGalleryItem(item: GalleryItem) {
            galleryItem = item
        }

        override fun onClick(v: View?) {
            //region Using Implicity intent to display a WebPage
            /*
            val intent = Intent(Intent.ACTION_VIEW, galleryItem.photoPageUri).also {
                startActivity(it)
            }*/
            //endregion
            //region Using a call to Activity to display a WebPage
            /*
            val intent = PhotoPageActivity.newIntent(requireContext(), galleryItem.photoPageUri)
            startActivity(intent)*/
            //endregion
            //region Using NavController to navigate to PhotoPageFragent and display a webPage
            /*     val bundle = bundleOf(
                     ARG_URI to galleryItem.photoPageUri
                 )
                 navController!!.navigate(
                     R.id.action_photoGalleryFragment_to_photoPageFragment,
                     bundle
                 )*/
            //endregion
            //region Using Chrome custom tabs to display a webPage. When an intent is fired, select Google Chrome as the Browser
            CustomTabsIntent.Builder()
                .setDefaultColorSchemeParams(
                    CustomTabColorSchemeParams.Builder().setToolbarColor(
                        ContextCompat.getColor(
                            requireContext(), R.color.teal_700
                        )
                    ).build()
                )
                .setShowTitle(true)
                .build()
                .launchUrl(requireContext(), galleryItem.photoPageUri)
            //endregion
        }

        /**
         * This method is used when I intend using Glide library to simplify my work
         * ///////////////
        fun bindGalleryItem(galleryItem: GalleryItem) {
        val imageView = itemView.findViewById<AppCompatImageView>(R.id.imageView_photoholder)

        Glide.with(photoGalleryFragmentContext).load(galleryItem.url)
        .placeholder(R.drawable.ic_baseline_photo_size_select_actual_24)
        .error(R.drawable.ic_baseline_photo_size_select_actual_24).into(imageView)

        }*/
    }

    /**
     * The Adapter class for the [photoRecyclerView]
     */
    private inner class PhotoAdapter(private val galleryItems: List<GalleryItem>) :
        RecyclerView.Adapter<PhotoHolder>() {

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): PhotoHolder {
            val view = layoutInflater.inflate(
                R.layout.list_item_gallery,
                parent,
                false
            ) as AppCompatImageView
            return PhotoHolder(view)
        }

        override fun getItemCount(): Int = galleryItems.size

        override fun onBindViewHolder(holder: PhotoHolder, position: Int) {

            val galleryItem = galleryItems[position]

            holder.bindGalleryItem(galleryItem)

            val placeHolder: Drawable = ContextCompat.getDrawable(
                requireContext(),
                R.drawable.ic_baseline_photo_size_select_actual_24
            ) ?: ColorDrawable()

            //attach this holder to the Thumbnail downloader
            thumbnailDownloader.queueThumbnail(holder, galleryItem.url, requireContext())
            //the placeholder is used before the background download by thumbnailDownloader attaches the downloaded image after it finish downloading the image
            holder.bindDrawable(placeHolder)
            //if i instead want to use Glide
            //holder.bindGalleryItem(galleryItem)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        //detach thumbnaildDownloader lifecycle from PhotoGalleryFragment viewlifecycle
        viewLifecycleOwner.lifecycle.removeObserver(
            thumbnailDownloader.viewLifecycleObserver
        )
    }

    override fun onDestroy() {
        //detach thumbnaildDownloader lifecycle from PhotoGalleryFragment lifecycle
        lifecycle.removeObserver(
            thumbnailDownloader.fragmentLifecycleObserver
        )
        super.onDestroy()
    }

    companion object {
        fun newInstance() = PhotoGalleryFragment()
}
}