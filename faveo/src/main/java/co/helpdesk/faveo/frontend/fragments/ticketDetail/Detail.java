package co.helpdesk.faveo.frontend.fragments.ticketDetail;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import co.helpdesk.faveo.Constants;
import co.helpdesk.faveo.Helper;
import co.helpdesk.faveo.R;
import co.helpdesk.faveo.backend.api.v1.Helpdesk;
import co.helpdesk.faveo.frontend.activities.SplashActivity;
import co.helpdesk.faveo.frontend.activities.TicketDetailActivity;

import org.json.JSONException;
import org.json.JSONObject;

public class Detail extends Fragment {
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    TextView textViewOpenedBy;

    EditText editTextSubject, editTextName, editTextEmail,
            editTextLastMessage, editTextDueDate, editTextCreatedDate, editTextLastResponseDate;

    Spinner spinnerSLAPlans, spinnerDepartment, spinnerStatus, spinnerSource,
            spinnerPriority, spinnerHelpTopics, spinnerAssignTo, spinnerChangeStatus;
    ProgressDialog progressDialog;

    Button buttonSave;

    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    public static Detail newInstance(String param1, String param2) {
        Detail fragment = new Detail();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    public Detail() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_detail, container, false);
        setUpViews(rootView);
        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setMessage("Fetching detail");
        progressDialog.show();
        new FetchTicketDetail(getActivity(), TicketDetailActivity.ticketID).execute();
        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                progressDialog.setMessage("Updating ticket");
                progressDialog.show();
                new SaveTicket(getActivity(),
                        Integer.parseInt(TicketDetailActivity.ticketID),
                        editTextSubject.getText().toString(),
                        spinnerSLAPlans.getSelectedItemPosition(),
                        spinnerHelpTopics.getSelectedItemPosition(),
                        spinnerSource.getSelectedItemPosition(),
                        spinnerPriority.getSelectedItemPosition()).execute();
            }
        });
        return rootView;
    }

    public class FetchTicketDetail extends AsyncTask<String, Void, String> {
        Context context;
        String ticketNumber;

        public FetchTicketDetail(Context context, String ticketNumber) {
            this.context = context;
            this.ticketNumber = ticketNumber;
        }

        protected String doInBackground(String... urls) {
            return new Helpdesk().getTicketDetail(ticketNumber);
        }

        protected void onPostExecute(String result) {
            progressDialog.dismiss();
            if (result == null) {
                Toast.makeText(getActivity(), "Something went wrong", Toast.LENGTH_LONG).show();
                return;
            }

            JSONObject jsonObject;
            try {
                JSONObject jsonObjectResult = new JSONObject(result);
                jsonObject = jsonObjectResult.getJSONObject("result");
                editTextSubject.setText(TicketDetailActivity.ticketSubject);

                try {
                    spinnerSLAPlans.setSelection(Integer.parseInt(jsonObject.getString("sla")));
                } catch(Exception e) { }

                try {
                    spinnerStatus.setSelection(Integer.parseInt(jsonObject.getString("status")) - 1);
                } catch(Exception e) { }

                try {
                    spinnerPriority.setSelection(Integer.parseInt(jsonObject.getString("priority_id")) - 1);
                } catch(Exception e) { }

                try {
                    spinnerDepartment.setSelection(Integer.parseInt(jsonObject.getString("dept_id")) - 1);
                } catch(Exception e) { }

                try {
                    spinnerHelpTopics.setSelection(Integer.parseInt(jsonObject.getString("help_topic_id")) - 1);
                } catch(Exception e) { }

                String assignedTo = jsonObject.getString("assigned_to");
                if (assignedTo.equals("null") || assignedTo.equals("")) {
                    editTextName.setText("Not assigned");
                }
                try {
                    editTextEmail.setText(jsonObject.getString("email"));
                } catch (JSONException e) {
                    editTextEmail.setText("Not available");
                }

                try {
                    spinnerSource.setSelection(Integer.parseInt(jsonObject.getString("source")) - 1);
                } catch(Exception e) { }

                String lastMessage = jsonObject.getString("last_message");
                if (lastMessage.equals("null") || lastMessage.equals("")) {
                    editTextLastMessage.setText("No last message");
                }

                editTextDueDate.setText(Helper.parseDate(jsonObject.getString("duedate")));
                editTextCreatedDate.setText(Helper.parseDate(jsonObject.getString("created_at")));
                editTextLastResponseDate.setText(Helper.parseDate(jsonObject.getString("updated_at")));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public class SaveTicket extends AsyncTask<String, Void, String> {
        Context context;
        int ticketNumber;
        String subject;
        int slaPlan;
        int helpTopic;
        int ticketSource;
        int ticketPriority;

        public SaveTicket(Context context, int ticketNumber, String subject,
                          int slaPlan, int helpTopic, int ticketSource, int ticketPriority) {
            this.context = context;
            this.ticketNumber = ticketNumber;
            this.subject = subject;
            this.slaPlan = slaPlan;
            this.helpTopic = helpTopic;
            this.ticketSource = ticketSource;
            this.ticketPriority = ticketPriority;
        }

        protected String doInBackground(String... urls) {
            if (subject.equals("Not available"))
                subject = "";
            return new Helpdesk().postEditTicket(ticketNumber, subject, slaPlan,
                    helpTopic, ticketSource, ticketPriority);
        }

        protected void onPostExecute(String result) {
            progressDialog.dismiss();
            if (result == null) {
                Toast.makeText(getActivity(), "Something went wrong", Toast.LENGTH_LONG).show();
                return;
            }
            if (result.contains("ticket_id"))
                Toast.makeText(getActivity(), "Update successful", Toast.LENGTH_LONG).show();
            else
                Toast.makeText(getActivity(), "Failed to update ticket", Toast.LENGTH_LONG).show();
        }
    }

    private void setUpViews(View rootView) {
        textViewOpenedBy = (TextView) rootView.findViewById(R.id.textView_opened_by);
        textViewOpenedBy.setText(TicketDetailActivity.ticketOpenedBy);
        editTextSubject = (EditText) rootView.findViewById(R.id.editText_subject);

        spinnerSLAPlans = (Spinner) rootView.findViewById(R.id.spinner_sla_plans);
        ArrayAdapter<String> spinnerArrayAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, SplashActivity.valueSLA.split(",")); //selected item will look like a spinner set from XML
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSLAPlans.setAdapter(spinnerArrayAdapter);

        spinnerStatus = (Spinner) rootView.findViewById(R.id.spinner_status);
        spinnerArrayAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, SplashActivity.valueStatus.split(",")); //selected item will look like a spinner set from XML
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(spinnerArrayAdapter);

        spinnerPriority = (Spinner) rootView.findViewById(R.id.spinner_priority);
        spinnerArrayAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, SplashActivity.valuePriority.split(",")); //selected item will look like a spinner set from XML
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerPriority.setAdapter(spinnerArrayAdapter);

        spinnerDepartment = (Spinner) rootView.findViewById(R.id.spinner_department);
        spinnerArrayAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, SplashActivity.valueDepartment.split(",")); //selected item will look like a spinner set from XML
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerDepartment.setAdapter(spinnerArrayAdapter);

        spinnerHelpTopics = (Spinner) rootView.findViewById(R.id.spinner_help_topics);
        spinnerArrayAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, SplashActivity.valueTopic.split(",")); //selected item will look like a spinner set from XML
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerHelpTopics.setAdapter(spinnerArrayAdapter);

        editTextName = (EditText) rootView.findViewById(R.id.editText_name);
        editTextEmail = (EditText) rootView.findViewById(R.id.editText_email);

        spinnerSource = (Spinner) rootView.findViewById(R.id.spinner_source);
        spinnerArrayAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, SplashActivity.valueSource.split(",")); //selected item will look like a spinner set from XML
        spinnerArrayAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSource.setAdapter(spinnerArrayAdapter);

        editTextLastMessage = (EditText) rootView.findViewById(R.id.editText_last_message);
        editTextDueDate = (EditText) rootView.findViewById(R.id.editText_due_date);
        editTextCreatedDate = (EditText) rootView.findViewById(R.id.editText_created_date);
        editTextLastResponseDate = (EditText) rootView.findViewById(R.id.editText_last_response_date);
        spinnerAssignTo = (Spinner) rootView.findViewById(R.id.spinner_assign_to);
        spinnerChangeStatus = (Spinner) rootView.findViewById(R.id.spinner_change_status);
        buttonSave = (Button) rootView.findViewById(R.id.button_save);
    }

    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnFragmentInteractionListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentInteractionListener {
        public void onFragmentInteraction(Uri uri);
    }

}
