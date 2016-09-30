package com.example.android.booklisting;

import android.app.Activity;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;


public class BookAdapter extends ArrayAdapter<Book> {
    public BookAdapter(Activity context, ArrayList<Book> books) {
        super(context, 0, books);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View listItemView = convertView;
        if (listItemView == null) {
            listItemView = LayoutInflater.from(getContext()).inflate(R.layout.book_items, parent, false);
            final Book currentBook = getItem(position);

            TextView titleText = (TextView) listItemView.findViewById(R.id.title_text_view);
            titleText.setText(currentBook.getTitle());

            TextView authorText = (TextView) listItemView.findViewById(R.id.author_text_view);
            authorText.setText(currentBook.getAuthor());


        }
        return listItemView;
    }

}
