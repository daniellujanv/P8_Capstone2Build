package dlujanapps.mx.wary.fragments;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import dlujanapps.mx.wary.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link HelpFeedbackFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class HelpFeedbackFragment extends Fragment {

    public HelpFeedbackFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment HelpFeedbackFragment.
     */
    public static HelpFeedbackFragment newInstance() {
        HelpFeedbackFragment fragment = new HelpFeedbackFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_help_feedback, container, false);
    }

}
