package com.example.nfc;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

public class DBHelper extends SQLiteOpenHelper {
    //----------------------------------Création et initialisation d'une base de données
    public DBHelper(Context context) {
        super(context, "Userdata.db", null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase DB) {
        DB.execSQL("create Table Userdetails(nom TEXT primary key, identifiant TEXT, present BOOLEAN )");
    }

    @Override
    public void onUpgrade(SQLiteDatabase DB, int i, int i1) {
        DB.execSQL("drop Table if exists Userdetails");
    }
    //insérer des utilisateurs
    public Boolean insertuserdata(String nom, String identifiant, Boolean present){
        SQLiteDatabase DB = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("nom", nom);
        contentValues.put("identifiant", identifiant);
        contentValues.put("present", present ? 1 : 0); // store Boolean as integer value
        long resultat = DB.insert("Userdetails", null, contentValues);
        if(resultat==-1){
            Log.d("TAG", "Failed to insert data");
            return false;
        } else {
            Log.d("TAG", "Data inserted successfully");
            return true;
        }
    }
    //Regarder si l'utilisateur est déja utilisé
    public boolean checkIfExists(String identifiant) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM Userdetails WHERE identifiant = ?", new String[]{identifiant});
        boolean exists = (cursor.getCount() > 0);
        cursor.close();
        return exists;
    }
    //retourner les données de la bdd
    public Cursor getData() {
        SQLiteDatabase DB = this.getWritableDatabase();
        Cursor cursor = DB.rawQuery("Select * from Userdetails", null);
        return cursor;
    }
    //Mettre l'utilisateur présent
    public boolean updateUserPresentByIdentifiant(String identifiant) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("present", true);
        int rowsUpdated = db.update("Userdetails", contentValues, "identifiant = ?", new String[] { identifiant });
        if (rowsUpdated > 0) {
            Log.d("TAG", "Data updated successfully");
            return true;
        } else {
            Log.d("TAG", "Failed to update data");
            return false;
        }
    }
    //Mettre l'utilisateur absent
    public boolean updateUserPresentByIdentifiant2(String identifiant) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("present", false);
        int rowsUpdated = db.update("Userdetails", contentValues, "identifiant = ?", new String[] { identifiant });
        if (rowsUpdated > 0) {
            Log.d("TAG", "Data updated successfully");
            return true;
        } else {
            Log.d("TAG", "Failed to update data");
            return false;
        }
    }
}
