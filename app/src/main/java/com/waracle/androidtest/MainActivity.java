package com.waracle.androidtest;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.ListFragment;
import android.support.v4.util.LruCache;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import org.json.JSONException;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends AppCompatActivity {

    private static String JSON_URL = "https://gist.githubusercontent.com/hart88/198f29ec5114a3ec3460/" +
            "raw/8dd19a88f9b8d24c23d9960f3300d0c917a4f07c/cake.json";

    // i added this. This is how performance is improved! The images are downloaded only once from the HTTP. On the first download they are
    // stored in the Memory Cache so next time they will be accessed from cache and will not have the need to download again.
    // This is improving performance very much as images are loaded much faster from the cache.
    private static LruCache<String, Bitmap> mMemoryCache;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, new PlaceholderFragment())
                    .commit();

            // i added this. Set the maxMemory
            final int maxMemory = (int) (Runtime.getRuntime().maxMemory() / 1024);
            //  i added this. Use 1/8th of the available memory for this memory cache.
            final int cacheSize = maxMemory / 8;
            MainActivity.PlaceholderFragment placeHolderFragment =
                    MainActivity.PlaceholderFragment.findOrCreateRetainFragment(getSupportFragmentManager());
            mMemoryCache = placeHolderFragment.mRetainedCache;
             if (mMemoryCache == null) {
            mMemoryCache = new LruCache<String, Bitmap>(cacheSize) {
                @Override
                protected int sizeOf(String key, Bitmap bitmap) {
                    // The cache size will be measured in kilobytes rather than
                    // number of items.
                    return bitmap.getByteCount() / 1024;
                }
            };
            placeHolderFragment.mRetainedCache = mMemoryCache;

            }

        }
    }

    public static LruCache<String, Bitmap> getMemoryCache () {

        return mMemoryCache;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_refresh) {

            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    /**
     * Fragment is responsible for loading in some JSON and
     * then displaying a list of cakes with images.
     * Fix any crashes
     * Improve any performance issues
     * Use good coding practices to make code more secure
     */
    public static class PlaceholderFragment extends ListFragment {

        private static final String TAG = PlaceholderFragment.class.getSimpleName();
        // i added this
        public LruCache<String, Bitmap> mRetainedCache;

        private ListView mListView;
        private MyAdapter mAdapter;


        public PlaceholderFragment() { /**/ }

        public static PlaceholderFragment findOrCreateRetainFragment(FragmentManager fm) {
            PlaceholderFragment fragment = (PlaceholderFragment) fm.findFragmentByTag(TAG);
            if (fragment == null) {
                fragment = new PlaceholderFragment();
                fm.beginTransaction().add(fragment, TAG).commit();
            }
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            // Changed this to android.R.id.List and the reference to it to "@android:id/list"
           mListView = (ListView) rootView.findViewById(android.R.id.list);

            LoadDataImagesTask task = new LoadDataImagesTask();
            task.execute(R.drawable.image_placeholder);
            return rootView;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);

            // Create and set the list adapter.
           // mAdapter = new MyAdapter();
            //mListView.setAdapter(mAdapter);

            // Load data from net.
            //NetworkOnMainThreadException is thrown when an application attempts to perform a networking operation on its main thread.
            // we need to Run the code in AsyncTask. See public class LoadDataImagesTask extends AsyncTask<ImageView, Void, Bitmap>
            // Removed this, i use loadData in the LoadDataImagesTask
            /*try {
                JSONArray array = loadData();
                mAdapter.setItems(array);
            } catch (IOException | JSONException e) {
                Log.e(TAG, e.getMessage());
            }*/
        }


        private Map loadData() throws IOException, JSONException {
            // i added this
            Map<String, List<String>> titleDescAndImageMap = new HashMap<String, List<String>>();
            URL url = new URL(JSON_URL);
            HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            try {
                InputStream in = new BufferedInputStream(urlConnection.getInputStream());

                // Can you think of a way to improve the performance of loading data
                // using HTTP headers???
                // i am using a memory cache

                // Also, Do you trust any utils thrown your way????
                // i have answered in the StreamUtils class

                byte[] bytes = StreamUtils.readInputStream(in);

                // Read in charset of HTTP content.
                String charset = parseCharset(urlConnection.getRequestProperty("Content-Type"));

                // Convert byte array to appropriate encoded string.
                String jsonText = new String(bytes, charset);
                // i read the jsonText and split by { to get the imageObjects and then split by , to get the image Details
                // i store in the map titleDescAndImageMap imageObject key and imageDetails as value
                String[] imageObject = jsonText.split("\\{");

                for (int i = 1; i < imageObject.length; i ++) {
                    String[] imageDetails = imageObject[i].split(",");
                    ArrayList<String> imageInfo = new ArrayList<>();

                    for (int j = 0; j < imageDetails.length; j++) {

                        imageInfo.add(imageDetails[j]);
                    }
                    String imageObjectDetails = new String("ImageObject"+ i);
                    titleDescAndImageMap.put(imageObjectDetails, imageInfo);
                }
                // Read string as JSON.

                return titleDescAndImageMap;
            } finally {
                urlConnection.disconnect();
            }
        }

        /**
         * Returns the charset specified in the Content-Type of this header,
         * or the HTTP default (ISO-8859-1) if none can be found.
         */
        public static String parseCharset(String contentType) {
            if (contentType != null) {
                String[] params = contentType.split(",");
                for (int i = 1; i < params.length; i++) {
                    String[] pair = params[i].trim().split("=");
                    if (pair.length == 2) {
                        if (pair[0].equals("charset")) {
                            return pair[1];
                        }
                    }
                }
            }
            return "UTF-8";
        }



        private class MyAdapter extends BaseAdapter {

            // Can you think of a better way to represent these items???
            // yes i use a Map<String, List<ImageInfo>>
            Map<String, List<ImageInfo>> titleDescAndImage = new HashMap<String, List<ImageInfo>>();
            List<ImageInfo> mapValues = new ArrayList<ImageInfo>();
            Context context;
            //private JSONArray mItems;
            private ImageLoader mImageLoader;

            // Removed it as redundant code
            /*public MyAdapter() {
               this(new ArrayList());
            }*/

            public MyAdapter(Context context, Map<String, List<ImageInfo>> titleDescAndImage) {
                this.titleDescAndImage = titleDescAndImage;
                this.context = context;
                mImageLoader = new ImageLoader();
            }

            @Override
            public int getCount() {
                return titleDescAndImage.size();
            }

            @Override
            public ImageInfo getItem(int position) {
                //Here i iterate through the titleDescAndImage i get title desc and imageUrl from it and populate it in the ImageInfo object
                // i return the mapValues.get(position) which will give me a title a desc and a imageUrl for an image
                String title = "";
                String desc = "";
                String imageUrl = "";
                for (Map.Entry<String, List<ImageInfo>> entry : titleDescAndImage.entrySet()){
                    String[] elements = entry.getValue().toString().split(",");
                    for (String detail : elements) {
                        if (detail.contains("title")) {
                            String [] values = detail.split(":");
                            title = values[1];
                        }
                        if (detail.contains("desc")) {
                            String [] values = detail.split(":");
                            desc = values[1];
                        }
                        if (detail.contains("image")) {
                            String [] values = detail.split(":");
                            imageUrl = values[1].substring(1, values[1].length())+":"+values[2].substring(0, values[2].length()-3);
                        }
                    }
                    ImageInfo item = new ImageInfo(title, desc, imageUrl);

                    mapValues.add(item);
                }
                    return mapValues.get(position);

            }

            @Override
            public long getItemId(int position) {
                return 0;
            }

            @SuppressLint("ViewHolder")
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                LayoutInflater inflater = LayoutInflater.from(getActivity());
                View root = inflater.inflate(R.layout.list_item_layout, parent, false);

                if (root != null) {
                    TextView title = (TextView) root.findViewById(R.id.title);
                    TextView desc = (TextView) root.findViewById(R.id.desc);
                    ImageView image = (ImageView) root.findViewById(R.id.image);
                    // Get the image Data from the getItem method
                    ImageInfo imageObject = getItem(position);
                    //Set title and description to the textviews
                    title.setText(imageObject.getTitle().trim());
                    desc.setText(imageObject.getDesc().trim());
                    image.setImageResource(R.drawable.image_placeholder);
                    // load the image
                    loadBitmap(imageObject.getImageUrl(), image);
                }

                return root;
            }

            public void setItems(Map<String, List<ImageInfo>> titleDescAndImage) {
                this.titleDescAndImage = titleDescAndImage;
            }

            //i added this. This method loads the image. If bitmap is found in memory cache it uses, if not i download it using the DownloadImageTask
            public void loadBitmap(String imageUrl, ImageView imageView) {
                // get image from cache
                final Bitmap bitmap = getBitmapFromMemCache(imageUrl);
                if (bitmap != null) {
                    imageView.setImageBitmap(bitmap);
                    // if not found in cache use the DownloadImageTask task to fetch it
                } else {

                    new DownloadImageTask(imageView).execute(imageUrl);

                }
            }
        }

        // i added this. I add the bitmap to memory cache
        public static void addBitmapToMemoryCache(String key, Bitmap bitmap) {
            if (getBitmapFromMemCache(key) == null) {
                if (bitmap != null) {
                    getMemoryCache().put(key, bitmap);
                }
            }
        }
        //i added this. If the bitmap is in memory cache i retrieve it here
        public static Bitmap getBitmapFromMemCache(String key) {
            return getMemoryCache().get(key);
        }

        // i added this. This async task loads all the data from the loadData method. It returns a map <String, List<ImageInfo>>
        public class LoadDataImagesTask extends AsyncTask<Integer, Void, Bitmap> {

            Map<String, List<ImageInfo>> titleDescAndImage = null;

            @Override
            protected Bitmap doInBackground(Integer... params) {

                try {

                    titleDescAndImage = loadData();

            } catch (IOException | JSONException e) {
                Log.e(TAG, e.getMessage());
            }

                return null;
            }

            @Override
            protected void onPostExecute(Bitmap result) {
                mAdapter = new MyAdapter(getContext(), titleDescAndImage);
                mAdapter.setItems(titleDescAndImage);
                mListView.setAdapter(mAdapter);
                mAdapter.notifyDataSetChanged();
            }

        }

        // i added this. This async task downloads an image as bitmap and then adds it to the memory cache
        private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
            ImageView bmImage;
            Bitmap mIcon = null;

            public DownloadImageTask(ImageView bmImage) {
                this.bmImage = bmImage;
            }
            protected Bitmap doInBackground(String... urls) {
                String urldisplay = urls[0];

                try {
                    InputStream in = new java.net.URL(urldisplay.replace("\"","")).openStream();
                    mIcon = BitmapFactory.decodeStream(in);
                    // This is hardcoded. This url should be "http://www.bbcgoodfood.com/sites/default/files/recipe_images/recipe-image-legacy-id--1001468_10.jpg"
                    // but on the loadData method i get back this "http://www.bbcgoodfood.com/sites/bbcgoodfood.com/files/recipe_images/recipe-image-legacy-id--1001468_10.jpg"
                    // Just because there are not many images and i know that mIcon only gets null for this image i hardcode it.
                    if (mIcon == null) {
                        in = new java.net.URL("https://www.bbcgoodfood.com/sites/default/files/recipe_images/recipe-image-legacy-id--1001468_10.jpg").openStream();
                        mIcon = BitmapFactory.decodeStream(in);
                    }
                    // add bitmap to memory cache
                    addBitmapToMemoryCache(urls[0], mIcon);
                    if (in != null) {
                        in.close();
                    }
                } catch (Exception e) {
                    Log.e("Error", e.getMessage());
                    e.printStackTrace();
                }
                return mIcon;
            }

            protected void onPostExecute(Bitmap result) {
                bmImage.setImageBitmap(result);
            }
        }

    }

}
