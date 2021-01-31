package com.example.myapplication;

import android.content.ContentUris;
import android.net.Uri;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ViewHolder> implements Filterable {
    List<Contact> Contacts;
    List<Contact>ContactsFull;
    private ContactsAdapter.OnItemListener onItemListener;

    public void setContacts(List<Contact> Contacts) {
        this.Contacts = Contacts;
        ContactsFull = new ArrayList<>(
                Contacts
        );
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.one_contact_row, parent, false);
        view.setOnClickListener(new ContactsAdapter.RV_ItemListener());
        view.setOnLongClickListener(new ContactsAdapter.RV_ItemListener());

        return new ViewHolder(view);

    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bindContact(Contacts.get(position));
        holder.itemView.setId(position);
    }


    @Override
    public int getItemCount() {
        return Contacts.size();
    }

    @Override
    public Filter getFilter() {
        return ContactFlter;
    }
    private Filter ContactFlter = new Filter() {
        @Override
        protected FilterResults performFiltering(CharSequence constraint) {
            List<Contact>FilterderContactsList = new ArrayList<>();
            if (constraint == null || constraint.length() == 0){
                FilterderContactsList.addAll(ContactsFull);
            }
            else {
                String FilterPattern = constraint.toString().toLowerCase().trim();
                for (Contact item: ContactsFull ){
                    if (item.name.toLowerCase().contains(FilterPattern)){
                        FilterderContactsList.add(item);
                    }
                }
            }
            FilterResults results = new FilterResults();
            results.values = FilterderContactsList;
            return results;
        }

        @Override
        protected void publishResults(CharSequence constraint, FilterResults results) {
            Contacts.clear();
            Contacts.addAll((List)results.values);
            notifyDataSetChanged();
        }
    };

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
    public  interface OnItemListener{
        void OnItemClickListener(View view, int position);
        void OnItemLongClickListener(View view, int position);
    }
    class RV_ItemListener implements View.OnClickListener, View.OnLongClickListener{

        @Override
        public void onClick(View view) {
            if (onItemListener != null){
                onItemListener.OnItemClickListener(view, view.getId());


            }
        }
        @Override
        public boolean onLongClick(View view) {
            if (onItemListener != null){
                onItemListener.OnItemLongClickListener(view,view.getId());
            }
            return true;
        }
    }
    public void setOnItemListenerListener(ContactsAdapter.OnItemListener listener){
        this.onItemListener = listener;
    }
}
