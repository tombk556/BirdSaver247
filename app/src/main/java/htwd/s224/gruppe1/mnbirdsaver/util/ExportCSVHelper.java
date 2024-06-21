/**
 * Helper class for exporting data from a Cursor to a CSV file.
 * This class supports creating a CSV file through an Activity or a Fragment.
 */
package htwd.s224.gruppe1.mnbirdsaver.util;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.opencsv.CSVWriter;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class ExportCSVHelper {
    private static final int CREATE_FILE_REQUEST_CODE = 1;
    private Activity activity;
    private Fragment fragment;
    private Cursor cursor;
    private int windTurbineId; // Optional filter
    private String csvDefaultName = "data_export";

    /**
     * Constructs an ExportCSVHelper for use in an Activity.
     *
     * @param activity The Activity context.
     * @param cursor The Cursor containing the data to export.
     */
    public ExportCSVHelper(Activity activity, Cursor cursor) {
        this.activity = activity;
        this.cursor = cursor;
    }

    /**
     * Constructs an ExportCSVHelper for use in a Fragment.
     *
     * @param fragment The Fragment context.
     * @param cursor The Cursor containing the data to export.
     */
    public ExportCSVHelper(Fragment fragment, Cursor cursor) {
        this.fragment = fragment;
        this.activity = fragment.getActivity();
        this.cursor = cursor;
    }

    /**
     * Sets the default name for the CSV file.
     *
     * @param name The default name for the CSV file.
     */
    public void setCSVDefaultNameName(String name) {
        this.csvDefaultName = name;
    }

    /**
     * Initiates the creation of a CSV file.
     * Opens a document creation activity for the user to choose the save location and file name.
     */
    public void createFile() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_TITLE, csvDefaultName + ".csv");
        if (fragment != null) {
            fragment.startActivityForResult(intent, CREATE_FILE_REQUEST_CODE);
        } else {
            activity.startActivityForResult(intent, CREATE_FILE_REQUEST_CODE);
        }
    }

    /**
     * Handles the result of the file creation activity.
     *
     * @param requestCode The request code passed to startActivityForResult.
     * @param resultCode The result code returned by the child activity.
     * @param data The Intent data returned by the child activity.
     */
    public void handleActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == CREATE_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                Uri uri = data.getData();
                writeFileToUri(uri);
            }
        }
    }

    /**
     * Writes the data from the Cursor to the specified URI as a CSV file.
     *
     * @param uri The URI to which the CSV file will be written.
     */
    private void writeFileToUri(Uri uri) {
        if (cursor != null && cursor.moveToFirst()) {
            try (OutputStream outputStream = activity.getContentResolver().openOutputStream(uri);
                 CSVWriter csvWriter = new CSVWriter(new OutputStreamWriter(outputStream))) {

                csvWriter.writeNext(cursor.getColumnNames());

                do {
                    String[] arrStr = new String[cursor.getColumnCount()];
                    for (int i = 0; i < cursor.getColumnCount(); i++) {
                        arrStr[i] = cursor.getString(i);
                    }
                    csvWriter.writeNext(arrStr);
                } while (cursor.moveToNext());

                cursor.close();
                Toast.makeText(activity, "Export successful! File saved.", Toast.LENGTH_LONG).show();
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(activity, "Export failed!", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(activity, "No data to export", Toast.LENGTH_SHORT).show();
        }
    }
}
