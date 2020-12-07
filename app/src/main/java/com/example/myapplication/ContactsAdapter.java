package com.example.myapplication;

import android.content.ContentUris;
import android.net.Uri;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ViewHolder> {
    List<Contact> Contacts = new ArrayList<>();

    public void setContacts(List<Contact> Contacts) {
        this.Contacts = Contacts;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.one_contact_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bindContact(Contacts.get(position));
    }


    @Override
    public int getItemCount() {
        return Contacts.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView ContactName, ContactNumber;
        ImageView ContactAvatar;

        public ViewHolder(@NonNull final View itemView) {
            super(itemView);
            ContactName = itemView.findViewById(R.id.ContactName);
            ContactNumber = itemView.findViewById(R.id.ContactNumber);
            ContactAvatar = itemView.findViewById(R.id.ContactAvatar);
        }

        public void bindContact(Contact contact) {
            ContactName.setText(contact.name);
            ContactNumber.setText(contact.number);
            Uri baseUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contact.id);
            Uri imageUri = Uri.withAppendedPath(baseUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
            ContactAvatar.setImageURI(imageUri);
        }
    }
}
