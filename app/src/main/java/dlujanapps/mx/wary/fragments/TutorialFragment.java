package dlujanapps.mx.wary.fragments;


import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.transition.Fade;
import android.transition.Scene;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.transition.TransitionSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import dlujanapps.mx.wary.MainActivity;
import dlujanapps.mx.wary.R;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link TutorialFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public class TutorialFragment extends DialogFragment {

    private String FIRST_OPEN_PREF_KEY = "firs_open_wary";

    public TutorialFragment() {
        // Required empty public constructor
    }

    private int step = 0;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment BlankFragment.
     */
    public static TutorialFragment newInstance() {
        return new TutorialFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences.Editor editor = PreferenceManager.
                getDefaultSharedPreferences(getContext()).edit();
        editor.putBoolean(FIRST_OPEN_PREF_KEY, false);
        editor.commit();
    }

    private ViewGroup mSceneRoot;
    private Scene mFirstScene;
    private Scene mSecondScene;
    private Scene mThirdScene;

    @Override
    public View onCreateView(LayoutInflater inflater
            , ViewGroup container, Bundle savedInstanceState) {
        mSceneRoot = (ViewGroup) inflater.inflate(R.layout.tutorial_scene, container, false);

        getDialog().requestWindowFeature(STYLE_NO_TITLE);

        mFirstScene = Scene.getSceneForLayout(
                mSceneRoot, R.layout.tutorial_1_fragment_start, getContext());
        mSecondScene = Scene.getSceneForLayout(
                mSceneRoot, R.layout.tutorial_2_fragment_friends, getContext());
        mThirdScene = Scene.getSceneForLayout(
                mSceneRoot, R.layout.tutorial_3_fragment_finder, getContext());

//        mSceneRoot.setOnClickListener(
//            clickListener
//        );

        mFirstScene.setEnterAction(mRunnable);
        mSecondScene.setEnterAction(mRunnable);
        mThirdScene.setEnterAction(mRunnable);

        TransitionManager.go(mFirstScene);

        return mSceneRoot;
    }

    Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            mSceneRoot.findViewById(R.id.tutorial_ok_button)
                    .setOnClickListener(clickListener);
            mSceneRoot.findViewById(R.id.tutorial_back_button)
                    .setOnClickListener(clickListener);
        }
    };


    View.OnClickListener clickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            switch(step){
                case 0:

                    if(v.getId() == R.id.tutorial_ok_button) {
                            TransitionManager.go(mSecondScene);
                    }
                    step = 1;
                    break;
                case 1:
                    if(v.getId() == R.id.tutorial_ok_button) {
                        TransitionManager.go(mThirdScene);
                        step = 2;
                    }else if(v.getId() == R.id.tutorial_back_button) {
                        TransitionManager.go(mFirstScene);
                        step = 0;
                    }
                    break;
                case 2:
                    if(v.getId() == R.id.tutorial_ok_button) {
                        dismiss();
                    }else if(v.getId() == R.id.tutorial_back_button) {
                        TransitionManager.go(mSecondScene);
                        step = 1;
                    }
                    break;
            }
        }
    };
}
