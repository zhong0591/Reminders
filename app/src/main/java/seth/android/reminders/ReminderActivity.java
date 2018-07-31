package seth.android.reminders;

import android.app.AlertDialog;
import android.app.Dialog;
import android.database.Cursor;
import android.os.Build;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;


public class ReminderActivity extends AppCompatActivity {
    private ListView mListView;
    public RemindersDbAdapter  mDbAdapter;
    private RemindersSimpleCursorAdapter mCursorAdapter;

    private void fireCustomDialog(final Reminder reminder){
// custom dialog
        final Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(seth.android.reminders.R.layout.dialog_custom);
        TextView titleView = (TextView) dialog.findViewById(seth.android.reminders.R.id.custom_title);
        final EditText editCustom = (EditText) dialog.findViewById(seth.android.reminders.R.id.custom_edit_reminder);
        Button commitButton = (Button) dialog.findViewById(seth.android.reminders.R.id.custom_button_commit);
        final CheckBox checkBox = (CheckBox) dialog.findViewById(seth.android.reminders.R.id.custom_check_box);
        LinearLayout rootLayout = (LinearLayout) dialog.findViewById(seth.android.reminders.R.id.custom_root_layout);
        final boolean isEditOperation = (reminder != null);
//this is for an edit
        if (isEditOperation){
            titleView.setText("Edit Reminder");
            checkBox.setChecked(reminder.getImportant() == 1);
            editCustom.setText(reminder.getContent());
            rootLayout.setBackgroundColor(getResources().getColor(seth.android.reminders.R.color.blue));
        }
        commitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String reminderText = editCustom.getText().toString();
                if (isEditOperation) {
                    Reminder reminderEdited = new Reminder(reminder.getId(),
                            reminderText, checkBox.isChecked() ? 1 : 0);
                    mDbAdapter.updateReminder(reminderEdited);
//this is for new reminder
                } else {
                    mDbAdapter.createReminder(reminderText, checkBox.isChecked());
                }
                mCursorAdapter.changeCursor(mDbAdapter.fetchAllReminders());
                dialog.dismiss();
            }
        });Button buttonCancel = (Button) dialog.findViewById(seth.android.reminders.R.id.custom_button_cancel);
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
        dialog.show();
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setHomeButtonEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setIcon(seth.android.reminders.R.mipmap.ic_launcher);

        //  mListView = (ListView) findViewById(R.id.remainders_list_view);
        //  String[] arr =    new String[]{"first record", "second record", "third record"};
        //  ArrayAdapter adapter = new ArrayAdapter<String>(this, R.layout.reminders_row, R.id.row_text, arr);
        mListView = (ListView) findViewById(R.id.remainders_list_view);
        mListView.setDivider(null);
        mDbAdapter = new RemindersDbAdapter(this);
        mDbAdapter.open();

        Cursor cursor = mDbAdapter.fetchAllReminders();

        String[] from = new String[]{
                RemindersDbAdapter.COL_CONTENT
        };
        int[] to = new int[]{
                seth.android.reminders.R.id.row_text
        };
        mCursorAdapter = new RemindersSimpleCursorAdapter(
                ReminderActivity.this,
                seth.android.reminders.R.layout.reminders_row,
                cursor,
                from,
                to,
                0);
        mListView.setAdapter(mCursorAdapter);

        if (savedInstanceState == null) {
            mDbAdapter.deleteAllReminders();
            //insertSomeReminders();
        }

        mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        mListView.setOnItemClickListener(
                new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, final int masterListPosition, long id) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(ReminderActivity.this);
                        ListView modeListView = new ListView(ReminderActivity.this);
                        String[] modes = new String[] { "Edit Reminder", "Delete Reminder" };
                        ArrayAdapter<String> modeAdapter = new ArrayAdapter<>(ReminderActivity.this,
                                android.R.layout.simple_list_item_1, android.R.id.text1, modes);
                        modeListView.setAdapter(modeAdapter);
                        builder.setView(modeListView);
                        final Dialog dialog = builder.create();
                        dialog.show();
                        modeListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                            @Override
                            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                if (position == 0) {
                                    int nId = getIdFromPosition(masterListPosition);
                                    Reminder reminder = mDbAdapter.fetchReminderById(nId);
                                    fireCustomDialog(reminder);
                                } else {
                                    mDbAdapter.deleteReminderById(getIdFromPosition(masterListPosition));
                                    mCursorAdapter.changeCursor(mDbAdapter.fetchAllReminders());
                                }
                                dialog.dismiss();
                            }
                        });
                    }
                }
        );



        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            mListView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
            mListView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
                @Override
                public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean
                        checked) {

                }
                @Override
                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                    MenuInflater inflater = mode.getMenuInflater();
                    inflater.inflate(seth.android.reminders.R.menu.cam_menu, menu);
                    return true;
                }
                @Override
                public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                    return false;
                }
                @Override
                public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                    switch (item.getItemId()) {
                        case seth.android.reminders.R.id.menu_item_delete_reminder:
                            for (int nC = mCursorAdapter.getCount() - 1; nC >= 0; nC--) {
                                if (mListView.isItemChecked(nC)) {
                                    mDbAdapter.deleteReminderById(getIdFromPosition(nC));
                                }
                            }
                            mode.finish();
                            mCursorAdapter.changeCursor(mDbAdapter.fetchAllReminders());
                            return true;
                    }
                    return false;
                }
                @Override
                public void onDestroyActionMode(ActionMode mode) { }
            });
        }

        mListView.setAdapter(mCursorAdapter);
    }

    private int getIdFromPosition(int nC) {
        return (int)mCursorAdapter.getItemId(nC);
    }

//    private void insertSomeReminders() {
//        mDbAdapter.createReminder("Send Dad birthday gift", false);
//        mDbAdapter.createReminder("Dinner at the Gage on Friday", false);
//        mDbAdapter.createReminder("String squash racket", false);
//        mDbAdapter.createReminder("Shovel and salt walkways", false);
//        mDbAdapter.createReminder("Prepare Advanced Android syllabus", true);
//        mDbAdapter.createReminder("Buy new office chair", false);
//        mDbAdapter.createReminder("Call Auto-body shop for quote", false);
//        mDbAdapter.createReminder("Renew membership to club", false);
//        mDbAdapter.createReminder("Buy new Galaxy Android phone", true);
//        mDbAdapter.createReminder("Sell old Android phone - auction", false);
//        mDbAdapter.createReminder("Buy new paddles for kayaks", false);
//        mDbAdapter.createReminder("Call accountant about tax returns", false);
//        mDbAdapter.createReminder("Buy 300,000 shares of Google", false);
//        mDbAdapter.createReminder("Call the Dalai Lama back", true);
//    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(seth.android.reminders.R.menu.menu_reminders, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case seth.android.reminders.R.id.action_new:
                fireCustomDialog(null);
                return true;
            case seth.android.reminders.R.id.action_exit:
                finish();
                return true;
            default:
                return false;
        }
    }
}
