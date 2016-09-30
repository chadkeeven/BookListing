package com.example.android.booklisting;


import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static android.os.Build.VERSION_CODES.M;


public class MainActivity extends AppCompatActivity {

    public static final String LOG_TAG = MainActivity.class.getSimpleName();
    public String GOOGLE_BOOK_URL = "https://www.googleapis.com/books/v1/volumes?q=";
    ArrayList<Book> books = new ArrayList<>();



    /**
     * Returns true if network is available or about to become available
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }
    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelableArrayList("books", books);
        super.onSaveInstanceState(outState);
    }




    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(savedInstanceState == null || !savedInstanceState.containsKey("books")) {
            books = new ArrayList(Arrays.asList(books));
        }
        else {
            books = savedInstanceState.getParcelableArrayList("books");
        }

        setContentView(R.layout.activity_main);




        ImageView searchBooks = (ImageView) findViewById(R.id.search_image);
        searchBooks.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                EditText searchEditText = (EditText) findViewById(R.id.search_edit_text);
                String searchKeyword = searchEditText.getText().toString();

                while (searchKeyword.contains(" ")) {
                    searchKeyword = searchKeyword.replace(" ", "");
                }
                String googlePlusSearch = GOOGLE_BOOK_URL + searchKeyword;

                if (isNetworkAvailable(MainActivity.this)) {

                    BookAsyncTask bookAsyncTask = new BookAsyncTask(googlePlusSearch);

                    bookAsyncTask.execute();
                } else {
                    Toast toast = Toast.makeText(MainActivity.this, R.string.no_internet, Toast.LENGTH_SHORT);
                    toast.show();
                }


            }
        });
    }


    public class BookAsyncTask extends AsyncTask<URL, Void, ArrayList<Book>> {


        ArrayList<Book> books = new ArrayList<>();
        private String mSearch;
        TextView emptyTextView = (TextView) findViewById(R.id.empty_text);


        public BookAsyncTask(String search) {
            mSearch = search;
        }

        /**
         * Update the screen with the given book (which was the result of the
         * {@link com.example.android.booklisting.MainActivity}).
         */
        @Override
        protected void onPostExecute(ArrayList<Book> books) {
            if (books == null) {
                emptyTextView.setText(R.string.empty_search);
                emptyTextView.setVisibility(View.VISIBLE);
                return;
            }

            BookAdapter adapter = new BookAdapter(MainActivity.this, books);


            ListView listView = (ListView) findViewById(R.id.list);


            if (books.size() > 0) {

                emptyTextView.setVisibility(View.GONE);

            } else {

                listView.setEmptyView(emptyTextView);
                emptyTextView.setText(R.string.no_book_info);
                emptyTextView.setVisibility(View.VISIBLE);
            }


            listView.setAdapter(adapter);
        }

        @Override
        protected ArrayList<Book> doInBackground(URL... urls) {
            // Create URL object
            URL url = createUrl(mSearch);

            // Perform HTTP request to the URL and receive a JSON response back
            String jsonResponse = "";
            try {
                jsonResponse = makeHttpRequest(url);
            } catch (IOException e) {
                Log.e(LOG_TAG, "Problem getting permission from HTTP url");
            }

            // Extract relevant fields from the JSON response and create an {@link Book} object
            books = extractItemsFromJson(jsonResponse);

            // Return the {@link Book} object as the result fo the {@link BookAsyncTask}
            return books;
        }


        /**
         * Returns new URL object from the given string URL.
         */
        private URL createUrl(String stringUrl) {
            URL url = null;
            try {
                url = new URL(stringUrl);
            } catch (MalformedURLException exception) {
                Log.e(LOG_TAG, "Error with creating URL", exception);
                return null;
            }
            return url;
        }

        /**
         * Make an HTTP request to the given URL and return a String as the response.
         */
        private String makeHttpRequest(URL url) throws IOException {
            String jsonResponse = "";
            if (url == null) {
                return jsonResponse;
            }
            HttpURLConnection urlConnection = null;
            InputStream inputStream = null;
            try {
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setReadTimeout(10000 /* milliseconds */);
                urlConnection.setConnectTimeout(15000 /* milliseconds */);
                urlConnection.connect();
                int code = urlConnection.getResponseCode();
                if (code == 200) {
                    inputStream = urlConnection.getInputStream();
                    jsonResponse = readFromStream(inputStream);
                } else {
                    Log.e(LOG_TAG, "Error response code: " + urlConnection.getResponseCode());
                }

            } catch (IOException e) {
                Log.e(LOG_TAG, "Problem retrieving the Google Book JSON results.", e);
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (inputStream != null) {
                    // function must handle java.io.IOException here
                    inputStream.close();
                }
            }
            return jsonResponse;
        }

        /**
         * Convert the {@link InputStream} into a String which contains the
         * whole JSON response from the server.
         */
        private String readFromStream(InputStream inputStream) throws IOException {
            StringBuilder output = new StringBuilder();
            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, Charset.forName("UTF-8"));
                BufferedReader reader = new BufferedReader(inputStreamReader);
                String line = reader.readLine();
                while (line != null) {
                    output.append(line);
                    line = reader.readLine();
                }
            }
            return output.toString();
        }

        /**
         * Return an {@link Book} object by parsing out information
         * about the first book from the input bookJSON string.
         */
        private ArrayList<Book> extractItemsFromJson(String bookJSON) {
            if (TextUtils.isEmpty(bookJSON)) {
                return null;
            }
            try {
                JSONObject baseJsonResponse = new JSONObject(bookJSON);
                JSONArray itemsArray = baseJsonResponse.getJSONArray("items");

                for (int j = 0; j < itemsArray.length(); j++) {
                    JSONObject currentItem = itemsArray.getJSONObject(j);
                    JSONObject volumeInfo = currentItem.getJSONObject("volumeInfo");
                    String title = "";
                    String author = "";
                    // Extract out the title and authors
                    title = volumeInfo.getString("title");
                    author = volumeInfo.getString("authors");
                    while (author.contains("\"") || author.contains("[") || author.contains("]")) {
                        author = author.replace("\"", "");
                        author = author.replace("[", "");
                        author = author.replace("]", "");
                    }

                    books.add(new Book(title, author));
                }
                // Create a new {@link Book} object
                return books;

            } catch (JSONException e) {
                Log.e(LOG_TAG, "Problem parsing the book JSON results", e);

            }
            return books;
        }


    }
}












