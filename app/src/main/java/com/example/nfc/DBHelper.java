package com.example.nfc;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import androidx.annotation.Nullable;

public class DBHelper extends SQLiteOpenHelper {
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

    public Boolean updateuserdata(String nom, String identifiant, String present){
        SQLiteDatabase DB = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put("nom", nom);
        contentValues.put("Identifiant", identifiant);
        contentValues.put("present", present);
        Cursor cursor = DB.rawQuery("Select * from Userdetails where identifiant = ?", new String[] {identifiant});
        if(cursor.getCount()>0){
            long resultat = DB.update("Userdetails", contentValues,"identifiant=?", new String[] {identifiant});
            if(resultat==-1){
                return false;
            }
            else {
                return true;
            }
        }
        else return false;
    }
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

    public Cursor getData() {
        SQLiteDatabase DB = this.getWritableDatabase();
        Cursor cursor = DB.rawQuery("Select * from Userdetails", null);
        return cursor;
    }
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
    public void deleteAllData() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM Userdetails");
    }

}
