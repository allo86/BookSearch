package com.codepath.android.booksearch.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.ShareActionProvider;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.codepath.android.booksearch.R;
import com.codepath.android.booksearch.models.Book;
import com.codepath.android.booksearch.net.BookClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import cz.msebera.android.httpclient.Header;

public class BookDetailActivity extends AppCompatActivity {
    public static final String BOOK = "BOOK";

    private ImageView ivBookCover;
    private TextView tvTitle;
    private TextView tvAuthor;

    private TextView tvWeight;
    private TextView tvPages;

    private Book book;

    private ShareActionProvider miShareAction;
    private Intent shareIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_detail);
        // Fetch views
        ivBookCover = (ImageView) findViewById(R.id.ivBookCover);
        tvTitle = (TextView) findViewById(R.id.tvTitle);
        tvAuthor = (TextView) findViewById(R.id.tvAuthor);
        tvWeight = (TextView) findViewById(R.id.tvWeight);
        tvPages = (TextView) findViewById(R.id.tvPages);

        // Extract book object from intent extras
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
            book = extras.getParcelable(BOOK);
        }

        // Use book object to populate data into views
        if (book != null) {
            // Change Toolbar title
            setTitle(book.getTitle());
            tvTitle.setText(book.getTitle());
            tvAuthor.setText(book.getAuthor());
            Picasso.with(this).load(book.getCoverUrl()).into(ivBookCover, new Callback() {
                @Override
                public void onSuccess() {
                    prepareShareIntent();
                    attachShareIntentAction();
                }

                @Override
                public void onError() {

                }
            });
        }

        fetchBookDetails();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_book_detail, menu);

        // Locate MenuItem with ShareActionProvider
        MenuItem item = menu.findItem(R.id.menu_item_share);
        // Fetch reference to the share action provider
        miShareAction = (ShareActionProvider) MenuItemCompat.getActionProvider(item);

        attachShareIntentAction();

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        /*
        if (id == R.id.action_settings) {
            return true;
        }
        */

        return super.onOptionsItemSelected(item);
    }

    public void prepareShareIntent() {
        // Fetch Bitmap Uri locally
        Uri bmpUri = getLocalBitmapUri(ivBookCover); // see previous remote images section
        // Construct share intent as described above based on bitmap
        shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, bmpUri);
        shareIntent.putExtra(Intent.EXTRA_TEXT, book.getTitle());
        shareIntent.setType("image/*");
    }

    public void attachShareIntentAction() {
        if (miShareAction != null && shareIntent != null)
            miShareAction.setShareIntent(shareIntent);
    }

    public Uri getLocalBitmapUri(ImageView imageView) {
        // Extract Bitmap from ImageView drawable
        Drawable drawable = imageView.getDrawable();
        Bitmap bmp = null;
        if (drawable instanceof BitmapDrawable) {
            bmp = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
        } else {
            return null;
        }

        // Store image to default external storage directory
        Uri bmpUri = null;
        try {
            File file = new File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "share_image_" + System.currentTimeMillis() + ".png");
            FileOutputStream out = new FileOutputStream(file);
            bmp.compress(Bitmap.CompressFormat.PNG, 90, out);
            out.close();
            bmpUri = Uri.fromFile(file);
        } catch (IOException e) {
            e.printStackTrace();

        }

        return bmpUri;
    }

    private void fetchBookDetails() {
        BookClient client = new BookClient();
        final String isbn = book.getIsbn();
        client.getBookDetails(isbn, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    if (response != null && response.has("ISBN:" + isbn)) {
                        JSONObject jsonObject = response.getJSONObject("ISBN:" + isbn);
                        if (jsonObject.has("weight"))
                            book.setWeight(jsonObject.getString("weight"));
                        if (jsonObject.has("number_of_pages"))
                            book.setPages(jsonObject.getInt("number_of_pages"));

                        if (book.getWeight() != null) {
                            tvWeight.setText(getString(R.string.weight, book.getWeight()));
                            tvWeight.setVisibility(View.VISIBLE);
                        } else {
                            tvWeight.setVisibility(View.GONE);
                        }

                        if (book.getPages() != 0) {
                            tvPages.setText(getString(R.string.pages, String.valueOf(book.getPages())));
                            tvPages.setVisibility(View.VISIBLE);
                        } else {
                            tvPages.setVisibility(View.GONE);
                        }
                    }
                } catch (JSONException e) {
                    // Invalid JSON format, show appropriate error.
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
                super.onFailure(statusCode, headers, responseString, throwable);
            }
        });
    }
}
