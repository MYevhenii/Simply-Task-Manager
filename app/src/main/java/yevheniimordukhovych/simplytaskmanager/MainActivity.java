package yevheniimordukhovych.simplytaskmanager;

import android.app.DatePickerDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{

    private Button BTNcurrentDay, BTNadd, BTNpreviousDay, BTNnextDay;
    private Calendar dateCalendar = Calendar.getInstance();

    private ListView LVmain;

    private ArrayList<String> tasks = new ArrayList<String>();
    private ArrayAdapter<String> adapter;

    final private String LOG_TAG = "myLogs";
    private DBHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BTNcurrentDay = (Button)findViewById(R.id.btn_current_day);
        BTNcurrentDay.setOnClickListener(this);

        BTNadd = (Button)findViewById(R.id.btn_add);
        BTNadd.setOnClickListener(this);

        BTNpreviousDay = (Button)findViewById(R.id.btn_previousDay);
        BTNpreviousDay.setOnClickListener(this);

        BTNnextDay = (Button)findViewById(R.id.btn_nextDay);
        BTNnextDay.setOnClickListener(this);

        setInitialDate();

        dbHelper = new DBHelper(this);

        adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_multiple_choice, tasks);

        LVmain = (ListView) findViewById(R.id.lv_list);
        LVmain.setAdapter(adapter);
        registerForContextMenu(LVmain);

        LVmain.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                setBox(String.valueOf(parent.getItemAtPosition(position)));
                adapter.notifyDataSetChanged();
            }
        });

        readSQL();
    }

    //Reading the database for the list
    public void readSQL(){
        tasks.removeAll(tasks);
        // подключаемся к БД
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        Log.d(LOG_TAG, "--- Rows in mytable: ---");

        String selection = null;
        String[] selectionArgs = null;
        String cDay = BTNcurrentDay.getText().toString();
        selection = "currentDay = ?";
        selectionArgs = new String[] { cDay };
        Cursor c = db.query("mytable", null, selection, selectionArgs, null, null, null);

        // ставим позицию курсора на первую строку выборки
        // если в выборке нет строк, вернется false
        if (c.moveToFirst()) {

            // определяем номера столбцов по имени в выборке
            int idColIndex = c.getColumnIndex("id");
            int currentDayColIndex = c.getColumnIndex("currentDay");
            int taskNameColIndex = c.getColumnIndex("taskName");
            int boxColIndex = c.getColumnIndex("box");

            int i = 0;
            do {
                // получаем значения по номерам столбцов и пишем все в лог
                Log.d(LOG_TAG,
                        "id = " + c.getInt(idColIndex) +
                                ", current day = " + c.getString(currentDayColIndex) +
                                ", taskName = " + c.getString(taskNameColIndex) +
                                ", box = " + c.getString(boxColIndex));
                tasks.add(new String(c.getString(taskNameColIndex)));

                if(c.getInt(boxColIndex)==1) {
                    LVmain.setItemChecked(i, true);
                    LVmain.setSelection(i);
                }
                else if(c.getInt(boxColIndex)==0) {
                    LVmain.setItemChecked(i, false);
                    LVmain.setSelection(i);
                }
                i++;
            } while (c.moveToNext());
        } else
            Log.d(LOG_TAG, "0 rows");
        c.close();

        // обновить список
        adapter.notifyDataSetChanged();
    }

    //adding data to the database
    public void addClick(){
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        final EditText ETtask = new EditText(this);

        alert.setTitle("Add new task");
        alert.setView(ETtask);
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String thisTask = ETtask.getText().toString();

                SQLiteDatabase db = dbHelper.getWritableDatabase();
                ContentValues cv = new ContentValues();

                Log.d(LOG_TAG, "--- Insert in mytable: ---");

                cv.put("currentDay", BTNcurrentDay.getText().toString());
                cv.put("taskName", thisTask);
                cv.put("box", false);

                long rowID = db.insert("mytable", null, cv);
                Log.d(LOG_TAG, "row inserted, ID = " + rowID);

                Cursor c = db.query("mytable", null, null, null, null, null, null);
                int taskNameColIndex = c.getColumnIndex("taskName");
                c.moveToLast();

                tasks.add(new String(c.getString(taskNameColIndex)));
                adapter.notifyDataSetChanged();
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });

        alert.show();
    }

    //set start dates
    private void setInitialDate() {
        BTNcurrentDay.setText(DateUtils.formatDateTime(this,
                dateCalendar.getTimeInMillis(),
                DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_SHOW_YEAR));
    }

    //setting the date picker
    DatePickerDialog.OnDateSetListener d = new DatePickerDialog.OnDateSetListener() {
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            dateCalendar.set(Calendar.YEAR, year);
            dateCalendar.set(Calendar.MONTH, monthOfYear);
            dateCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
            setInitialDate();
            readSQL();
        }
    };

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);

        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.context_menu, menu);
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        switch (item.getItemId()) {
            case R.id.edit:
                editItem(info.position); // метод, выполняющий действие при редактировании пункта меню
                return true;
            case R.id.delete:
                deleteItem(info.position); //метод, выполняющий действие при удалении пункта меню
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    //edit tsak
    public void editItem(final int position){
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        final EditText ETtask = new EditText(this);
        ETtask.setText(String.valueOf(LVmain.getItemAtPosition(position)));

        alert.setTitle("Edit task");
        alert.setView(ETtask);
        alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int whichButton) {
                SQLiteDatabase db = dbHelper.getWritableDatabase();
                ContentValues cv = new ContentValues();

                String thisTask = String.valueOf(LVmain.getItemAtPosition(position));
                String taskName = ETtask.getText().toString();
                String cDay = BTNcurrentDay.getText().toString();

                Log.d(LOG_TAG, "--- Update mytable: ---");
                // подготовим значения для обновления
                cv.put("taskName", taskName);
                cv.put("currentDay", cDay);
                // обновляем по taskName
                int updCount = db.update("mytable", cv, "taskName = ?",
                        new String[] { thisTask });
                Log.d(LOG_TAG, "updated rows count = " + updCount);
                tasks.set(position, taskName);
                adapter.notifyDataSetChanged();
            }
        });

        alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
            }
        });

        alert.show();
    }

    //delete task
    public void deleteItem(int position){
        Toast.makeText(MainActivity.this, "task was deleted", Toast.LENGTH_LONG).show();

        // подключаемся к БД
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.execSQL("DELETE FROM mytable WHERE taskName = '" + String.valueOf(LVmain.getItemAtPosition(position)) + "'");

        tasks.remove(position);
        adapter.notifyDataSetChanged();
    }

    //set values for box in the database
    public void checkbox(String taskName, int flag){
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues cv = new ContentValues();

        String cDay = BTNcurrentDay.getText().toString();

        Log.d(LOG_TAG, "--- Update mytable: ---");
        // подготовим значения для обновления
        cv.put("taskName", taskName);
        cv.put("currentDay", cDay);
        cv.put("box", flag);
        // обновляем по taskName
        int updCount = db.update("mytable", cv, "taskName = ?",
                new String[] { taskName });
        Log.d(LOG_TAG, "updated rows count = " + updCount);
    }

    //read and set the box in the database using the checkbox method
    public void setBox(String taskName){
        // подключаемся к БД
        SQLiteDatabase db = dbHelper.getWritableDatabase();

        Log.d(LOG_TAG, "--- Rows in mytable: ---");

        String selection = null;
        String[] selectionArgs = null;
        String cDay = BTNcurrentDay.getText().toString();
        selection = "currentDay = ? AND taskName = ?";
        selectionArgs = new String[] { cDay, taskName };
        Cursor c = db.query("mytable", null, selection, selectionArgs, null, null, null);

        // ставим позицию курсора на первую строку выборки
        // если в выборке нет строк, вернется false
        if (c.moveToFirst()) {

            // определяем номера столбцов по имени в выборке
            int idColIndex = c.getColumnIndex("id");
            int currentDayColIndex = c.getColumnIndex("currentDay");
            int taskNameColIndex = c.getColumnIndex("taskName");
            int boxColIndex = c.getColumnIndex("box");

            do {
                // получаем значения по номерам столбцов и пишем все в лог
                Log.d(LOG_TAG,
                        "id = " + c.getInt(idColIndex) +
                                ", current day = " + c.getString(currentDayColIndex) +
                                ", taskName = " + c.getString(taskNameColIndex) +
                                ", box = " + c.getString(boxColIndex));

                if (c.getInt(boxColIndex) == 0)
                    checkbox(taskName, 1);
                else
                    checkbox(taskName, 0);
            } while (c.moveToNext());
        } else
            Log.d(LOG_TAG, "0 rows");
        c.close();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_current_day:
                new DatePickerDialog(MainActivity.this, d,
                        dateCalendar.get(Calendar.YEAR),
                        dateCalendar.get(Calendar.MONTH),
                        dateCalendar.get(Calendar.DAY_OF_MONTH))
                        .show();
                break;
            case R.id.btn_add:
                addClick();
                break;
            case R.id.btn_previousDay:
                dateCalendar.setTimeInMillis(dateCalendar.getTimeInMillis() - 1000*60*60*24);
                setInitialDate();
                readSQL();
                break;
            case R.id.btn_nextDay:
                dateCalendar.setTimeInMillis(dateCalendar.getTimeInMillis() + 1000*60*60*24);
                setInitialDate();
                readSQL();
                break;
            default:
                break;
        }
    }
}
