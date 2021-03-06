package com.chrislydic.monitor.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.chrislydic.monitor.Alert;
import com.chrislydic.monitor.CoinFragment;
import com.chrislydic.monitor.Pair;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by chris on 1/15/2018.
 */

public class AlertHelper {
	private static AlertHelper alertHelper;

	private Context mContext;
	private SQLiteDatabase mDatabase;

	public static AlertHelper get( Context context ) {
		if ( alertHelper == null) {
			alertHelper = new AlertHelper(context);
		}

		return alertHelper;
	}

	private AlertHelper( Context context ) {
		mContext = context.getApplicationContext();
		mDatabase = new MonitorBaseHelper( mContext ).getWritableDatabase();
	}

	public List<Alert> getAlerts( Pair pair ) {
		List<Alert> alerts = new ArrayList<>();

		AlertCursorWrapper cursor = queryItems( AlertDbSchema.Table.Cols.PAIR + " = ?",
				new String[] { Long.toString( pair.getId() ) } );

		try {
			cursor.moveToFirst();
			while ( !cursor.isAfterLast() ) {
				alerts.add( cursor.getAlert() );
				cursor.moveToNext();
			}
		} finally {
			cursor.close();
		}

		return alerts;
	}

	public Alert getAlert( long id ) {
		AlertCursorWrapper cursor = queryItems( AlertDbSchema.Table.Cols.ID + " = ?",
				new String[] { Long.toString( id ) } );

		try {
			if ( cursor.getCount() == 0 ) {
				return null;
			}

			cursor.moveToFirst();
			return cursor.getAlert();
		} finally {
			cursor.close();
		}
	}

	public Alert addAlert( int action, double amount, long pairId, int type, int frequency ) {
		ContentValues values = new ContentValues();
		values.put( AlertDbSchema.Table.Cols.ACTION, action );
		values.put( AlertDbSchema.Table.Cols.AMOUNT, amount );
		values.put( AlertDbSchema.Table.Cols.PAIR, pairId );
		values.put( AlertDbSchema.Table.Cols.TYPE, type );
		values.put( AlertDbSchema.Table.Cols.FREQUENCY, frequency );
		values.put( AlertDbSchema.Table.Cols.ENABLED, true );
		values.put( AlertDbSchema.Table.Cols.ACTIVE, false );

		long id = mDatabase.insert( AlertDbSchema.Table.NAME, null, values );

		return new Alert( id, type, amount, action, pairId, frequency );
	}

	public void updateEnabled( long id, boolean enabled ) {
		ContentValues values = new ContentValues();
		values.put( AlertDbSchema.Table.Cols.ENABLED, enabled ? 1 : 0 );

		mDatabase.update( AlertDbSchema.Table.NAME,
				values,
				AlertDbSchema.Table.Cols.ID + " = ?",
				new String[]{ Long.toString( id ) } );
	}

	public void updateActive( long id, boolean active ) {
		ContentValues values = new ContentValues();
		values.put( AlertDbSchema.Table.Cols.ACTIVE, active ? 1 : 0 );

		mDatabase.update( AlertDbSchema.Table.NAME,
				values,
				AlertDbSchema.Table.Cols.ID + " = ?",
				new String[]{ Long.toString( id ) } );
	}

	public void deleteAlert( Alert alert, Context context ) {
		alert.cancelPriceAlert( context );

		mDatabase.delete( AlertDbSchema.Table.NAME,
				AlertDbSchema.Table.Cols.ID + " = ?",
				new String[]{ Long.toString( alert.getId() ) } );
	}

	private AlertCursorWrapper queryItems( String whereClause, String[] whereArgs ) {
		Cursor cursor = mDatabase.query(
				AlertDbSchema.Table.NAME,
				null, // columns -- null selects all columns
				whereClause,
				whereArgs,
				null, // groupby
				null, // having
				null  // orderby
		);

		return new AlertCursorWrapper( cursor );
	}
}
