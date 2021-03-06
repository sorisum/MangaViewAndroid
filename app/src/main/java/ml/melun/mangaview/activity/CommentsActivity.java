package ml.melun.mangaview.activity;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.design.widget.TabLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;

import ml.melun.mangaview.Preference;
import ml.melun.mangaview.R;
import ml.melun.mangaview.adapter.commentsAdapter;
import ml.melun.mangaview.fragment.CommentsTabFragment;
import ml.melun.mangaview.mangaview.Comment;
import ml.melun.mangaview.mangaview.Login;

import static ml.melun.mangaview.MainApplication.httpClient;
import static ml.melun.mangaview.MainApplication.p;
import static ml.melun.mangaview.Utils.writeComment;

public class CommentsActivity extends AppCompatActivity {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link FragmentPagerAdapter} derivative, which will keep every
     * loaded fragment in memory. If this becomes too memory intensive, it
     * may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    private SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    private ViewPager mViewPager;
    ArrayList<Comment> comments, bcomments;
    public commentsAdapter adapter, badapter;
    Context context;
    TabLayout tab;
    int id;
    ImageButton submit;
    EditText input;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if(p.getDarkTheme()) setTheme(R.style.AppThemeDarkNoTitle);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        context = this;
        //swipe = this.findViewById(R.id.commentSwipe);
        Intent intent = getIntent();
        tab = this.findViewById(R.id.tab_layout);

        String gsonData = intent.getStringExtra("comments");
        if(gsonData.length()>0){
            Gson gson = new Gson();
            comments = gson.fromJson(gsonData,new TypeToken<ArrayList<Comment>>(){}.getType());
            adapter = new commentsAdapter(context, comments);
            getSupportActionBar().setTitle("댓글 " + comments.size());
        }else{
            getSupportActionBar().setTitle("댓글 없음");
        }

        gsonData = intent.getStringExtra("bestComments");
        if(gsonData.length()>0){
            Gson gson = new Gson();
            bcomments = gson.fromJson(gsonData,new TypeToken<ArrayList<Comment>>(){}.getType());
            badapter = new commentsAdapter(context, bcomments);
            //((TextView)toolbar.findViewById(R.id.comments_title)).setText("댓글 ["+comments.size()+"]");
        }

        id = intent.getIntExtra("id",0);

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());



        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.container);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        mViewPager.requestFocus();

        tab.addTab(tab.newTab().setText("베스트 댓글"));
        tab.addTab(tab.newTab().setText("전체 댓글"));

        mViewPager.addOnPageChangeListener(new
                TabLayout.TabLayoutOnPageChangeListener(tab));
        tab.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                mViewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                //
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                //
            }
        });


        submit = this.findViewById(R.id.commentButton);
        input = this.findViewById(R.id.comment_editText);
        final Login login = p.getLogin();
        if(login != null && login.isValid()){
            submit.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (input.length() > 0) {
                        submit.setEnabled(false);
                        input.setEnabled(false);
                        new submitComment(login, id, input.getText().toString(), p.getUrl())
                                .executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    }
                }
            });
        }
        else {
            this.findViewById(R.id.comment_input).setVisibility(View.GONE);
        }

    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            CommentsTabFragment tab = new CommentsTabFragment();
            switch(position){
                case 0:
                    //best
                    tab.setAdapter(badapter);
                    return tab;
                case 1:
                    //comments
                    tab.setAdapter(adapter);
                    return tab;
            }
            return null;
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 2;
        }
    }

    private class submitComment extends AsyncTask<Void,Void,Integer> {
        Login login;
        int id;
        String baseUrl;
        String content;
        public submitComment(Login login, int id, String content, String baseUrl) {
            this.login = login;
            this.id = id;
            this.content = content;
            this.baseUrl = baseUrl;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onPostExecute(Integer integer) {
            super.onPostExecute(integer);
            if(integer == 0){
                //success
                //update login
                p.setLogin(login);
                comments.add(new Comment("나","","",content,0,0,0));
                adapter.notifyDataSetChanged();
                Toast.makeText(context, "댓글 등록 성공", Toast.LENGTH_SHORT).show();
                input.getText().clear();
            }else{
                Toast.makeText(context, "실패", Toast.LENGTH_SHORT).show();
                //failed
            }
            submit.setEnabled(true);
            input.setEnabled(true);

        }

        @Override
        protected Integer doInBackground(Void... voids) {
            if(writeComment(httpClient, login, id, content, baseUrl)) return 0;
            else{
                //login again and try again
                login.submit(httpClient);
                if(writeComment(httpClient, login, id, content, baseUrl)) return 0;
            }
            return 1;
        }
    }
}
