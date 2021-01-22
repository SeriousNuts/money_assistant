package com.example.myapplication;

import android.content.ContentUris;
import android.content.Context;
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

public class choosenContactsArapter extends RecyclerView.Adapter<choosenContactsArapter.choosenContactsViewHolder> {
    ArrayList<Contact> ChoosenContacts = new ArrayList<>();
    Context context;
    private int numberItems;
    String data1[],data2[];
    int images;
    public choosenContactsArapter(Context ct,ArrayList<Contact> ChoosenContacts ,int numberOfItems){
        numberItems= numberOfItems;
        this.ChoosenContacts = ChoosenContacts;
        context=ct;
    }
    @NonNull
    @Override
    public choosenContactsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        int layoutIdForListItem = R.layout.choosen_contacts_list_item;
        LayoutInflater inflater = LayoutInflater.from(context);

        View view =inflater.inflate(layoutIdForListItem,parent,false);
        return new choosenContactsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull choosenContactsViewHolder holder, int position) {
        holder.bindContact(ChoosenContacts.get(position));
        holder.itemView.setId(position);

    }

    @Override
    public int getItemCount() {
        return ChoosenContacts.size();
    }

   static class choosenContactsViewHolder extends RecyclerView.ViewHolder{
        TextView ChoosenContactPhone;
        TextView ChoosenContactName;
        ImageView ChoosenContactsImage;
        public choosenContactsViewHolder(@NonNull View itemView) {
            super(itemView);
            ChoosenContactPhone = itemView.findViewById(R.id.ChoosenContactPhone);
            ChoosenContactName = itemView.findViewById(R.id.ChoosenContactName);
            ChoosenContactsImage= itemView.findViewById(R.id.ChoosenContactImage);


        }
       public void bindContact(Contact contact) {
           ChoosenContactName.setText(contact.name);
           ChoosenContactPhone.setText(contact.number);
           Uri baseUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contact.id);
           Uri imageUri = Uri.withAppendedPath(baseUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
           ChoosenContactsImage.setImageURI(imageUri);

       }

    }
}
