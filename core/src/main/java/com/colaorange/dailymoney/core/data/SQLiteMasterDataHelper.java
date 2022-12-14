package com.colaorange.dailymoney.core.data;

import static com.colaorange.dailymoney.core.data.DataMeta.COL_ACC_CASHACCOUNT;
import static com.colaorange.dailymoney.core.data.DataMeta.COL_ACC_PRIORITY;
import static com.colaorange.dailymoney.core.data.DataMeta.TB_ACC;
import static com.colaorange.dailymoney.core.data.MasterDataMeta.*;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.colaorange.dailymoney.core.util.Logger;
import com.colaorange.dailymoney.core.context.Contexts;

/**
 * @author dennis
 */
public class SQLiteMasterDataHelper extends SQLiteOpenHelper {
    /**
     * maintain this field carefully
     */
//    private static final int VERSION = 1;//0.9.6--0.10.6
    private static final int VERSION = 2;//0.10.7-freshly-200207001 -

    private static final String BOOK_CREATE_SQL = "CREATE TABLE " + TB_BOOK + " ("
            + COL_BOOK_ID + " TEXT PRIMARY KEY, "
            + COL_BOOK_NAME + " TEXT NOT NULL, "
            + COL_BOOK_SYMBOL + " TEXT NULL, "
            + COL_BOOK_SYMBOL_POSITION + " INTEGER NOT NULL, "
            + COL_BOOK_PRIORITY + " INTEGER NULL, "
            + COL_BOOK_NOTE + " TEXT)";
    private static final String BOOK_DROP_SQL = "DROP TABLE IF EXISTS " + TB_BOOK;


    public SQLiteMasterDataHelper(Context context, String dbname) {
        super(context, dbname, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        Logger.d("create master schema {}", BOOK_CREATE_SQL);

        db.execSQL(BOOK_CREATE_SQL);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Logger.d("update master db from {} to {}", oldVersion, newVersion);

        if (oldVersion < 0) {
            Logger.i("reset master schema");
            //drop and create.
            Logger.i("drop master schema " + BOOK_DROP_SQL);
            db.execSQL(BOOK_DROP_SQL);
            onCreate(db);
            return;
        }

        //keep going check next id
        if (oldVersion == 1) {//schema before ?
            Logger.i("upgrade schem from " + oldVersion + " to " + newVersion);
            db.execSQL("ALTER TABLE " + TB_BOOK + " ADD " + COL_BOOK_PRIORITY + " INTEGER NULL ");
            oldVersion++;
        }
    }

    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Logger.i("downgrade db from {} to {}", oldVersion, newVersion);

        if (newVersion == 1) {
            Logger.i("downgrade schema from " + oldVersion + " to " + newVersion);
            db.execSQL("ALTER TABLE " + TB_BOOK + " DROP " + COL_BOOK_PRIORITY);

            newVersion--;
        }
    }

}
