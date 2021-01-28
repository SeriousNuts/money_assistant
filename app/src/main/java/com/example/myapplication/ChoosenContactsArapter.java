package com.example.myapplication;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.ContactsContract;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

public class ChoosenContactsArapter extends RecyclerView.Adapter<ChoosenContactsArapter.choosenContactsViewHolder> {
    ArrayList<Contact> ChoosenContacts = new ArrayList<>();
    static double AmountForEveryUserValue = -1;
    public void setChoosenContacts(ArrayList<Contact> ChoosenContacts) {
        this.ChoosenContacts = ChoosenContacts;
        notifyDataSetChanged();
    }

    public ArrayList<Contact> getChoosenContacts() {
        return ChoosenContacts;
    }

    public void setAmountForEveryUserValue(double AmountForEveryUserValue){
        ChoosenContactsArapter.AmountForEveryUserValue = AmountForEveryUserValue;
        notifyDataSetChanged();
        if (ChoosenContacts.size()!=0) {
            for (int i = 0; i < ChoosenContacts.size(); i++) {
                ChoosenContacts.get(i).amount = AmountForEveryUserValue;
            }
        }

    }
    @NonNull
    @Override
    public choosenContactsViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.choosen_contacts_list_item, parent, false);
        return new choosenContactsViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final choosenContactsViewHolder holder, final int position) {
        holder.bindContact(ChoosenContacts.get(position));
        holder.itemView.setId(position);
        holder.AddAmountBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(v.getRootView().getContext());
                @SuppressLint("ResourceType")View dialogView = LayoutInflater.from(v.getRootView().getContext()).inflate(R.layout.custom_dialog_for_add_amount,null);
                de.hdodenhof.circleimageview.CircleImageView dialogImageView = dialogView.findViewById(R.id.dialog_boxImage);
                builder.setView(dialogView);
                builder.setCancelable(true);
                TextView dialog_boxUsername = dialogView.findViewById(R.id.dialog_boxUserName);
                TextView dialog_boxPhone = dialogView.findViewById(R.id.dialog_boxUserPhone);
                Button ContinueBtn = dialogView.findViewById(R.id.ContinueBtn);
                Uri baseUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, ChoosenContacts.get(position).id);
                Uri imageUri = Uri.withAppendedPath(baseUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
                dialog_boxUsername.setText(ChoosenContacts.get(position).name);
                dialog_boxPhone.setText(ChoosenContacts.get(position).number);
                dialogImageView.setImageURI(imageUri);
                final AlertDialog AmountDialog = builder.show();
                final EditText AmountInput = dialogView.findViewById(R.id.dialog_UserAmountText);
                ContinueBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        holder.AmountForPersonalityUser.setText(AmountInput.getText().toString());
                        holder.AmountForEveryUser.setText("    ");
                        ChoosenContacts.get(position).amount = Double.parseDouble(AmountInput.getText().toString());
                        AmountDialog.cancel();
                    }
                });
            }
        });


    }

    @Override
    public int getItemCount() {
        return ChoosenContacts.size();
    }

   static class choosenContactsViewHolder extends RecyclerView.ViewHolder{
        TextView ChoosenContactPhone;
        TextView ChoosenContactName;
        TextView AmountForEveryUser;
        ImageView ChoosenContactsImage;
        FloatingActionButton AddAmountBtn;
        TextView AmountForPersonalityUser;



        public choosenContactsViewHolder(@NonNull View itemView) {
            super(itemView);
            ChoosenContactPhone = itemView.findViewById(R.id.ChoosenContactPhone);
            ChoosenContactName = itemView.findViewById(R.id.ChoosenContactName);
            ChoosenContactsImage= itemView.findViewById(R.id.circleImageView);
            AmountForEveryUser=itemView.findViewById(R.id.AmountForEveryUser_item);
            AddAmountBtn=itemView.findViewById(R.id.AddAmountBtn);
            AmountForPersonalityUser= itemView.findViewById(R.id.Pesonality);
        }

       public void bindContact(Contact contact) {
            if(AmountForEveryUserValue != -1){
                AmountForEveryUser.setText(String.valueOf(AmountForEveryUserValue));
                AmountForPersonalityUser.setText(" ");
            }
           ChoosenContactName.setText(contact.name);
           ChoosenContactPhone.setText(contact.number);
           Uri baseUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, contact.id);
           Uri imageUri = Uri.withAppendedPath(baseUri, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY);
           ChoosenContactsImage.setImageURI(imageUri);

       }


    }
}
