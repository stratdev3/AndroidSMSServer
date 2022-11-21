package github.umer0586.smsserver.activities;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import github.umer0586.smsserver.R;

public class FeedbackActivity extends AppCompatActivity {


    private static final String URL = "https://docs.google.com/forms/d/e/1FAIpQLSe6ZA9bu9sgzmhh3mJtZLN08Cb-NRaQMnBPiFGKA-AXisSGQQ/viewform";
    private static ViewPager2 viewPager;

    private static final int POSITION_LOADING_FRAGMENT = 2;
    private static final int POSITION_ERROR_FRAGMENT = 1;
    private static final int POSITION_WEBVIEW_FRAGMENT = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_feedback);

        viewPager = findViewById(R.id.viewpager);

        viewPager.setAdapter(new MyFragmentStateAdapter(this));
        viewPager.setUserInputEnabled(false);
    }

    public class MyFragmentStateAdapter extends FragmentStateAdapter {

        public MyFragmentStateAdapter(@NonNull FragmentActivity fragmentActivity)
        {
            super(fragmentActivity);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position)
        {
            switch(position)
            {
                case POSITION_LOADING_FRAGMENT:
                    return new LoadingFragment();
                case POSITION_ERROR_FRAGMENT:
                    return new ErrorFragment();
                case POSITION_WEBVIEW_FRAGMENT:
                    return new WebViewFragment();
            }

            return new ErrorFragment();
        }

        @Override
        public int getItemCount()
        {
            return 3;
        }
    }


    public static class LoadingFragment extends Fragment {

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
        {
            return inflater.inflate(R.layout.fragment_loading, container, false);
        }
    }

    public static class ErrorFragment extends Fragment {

        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
        {
            return inflater.inflate(R.layout.fragment_error, container, false);
        }

    }

    public static class WebViewFragment extends Fragment {

        private WebView webView;
        @Nullable
        @Override
        public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState)
        {
            return inflater.inflate(R.layout.fragment_webview, container, false);
        }

        @Override
        public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState)
        {
            super.onViewCreated(view, savedInstanceState);
            webView = view.findViewById(R.id.webview);
            webView.getSettings().setJavaScriptEnabled(true);
            webView.setWebViewClient(new MyWebViewClient());
            webView.loadUrl(URL);

        }
    }

    public static class MyWebViewClient extends WebViewClient {

        private static final String TAG = MyWebViewClient.class.getSimpleName();
        private boolean errorOccurred = false;

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon)
        {
            super.onPageStarted(view, url, favicon);
            Log.d(TAG, "onPageStarted()");
            viewPager.setCurrentItem(POSITION_LOADING_FRAGMENT,false);
        }

        @Override
        public void onPageFinished(WebView view, String url)
        {
            super.onPageFinished(view, url);
            Log.d(TAG, "onPageFinished()");

            if(!errorOccurred)
                viewPager.setCurrentItem(POSITION_WEBVIEW_FRAGMENT,false);
        }

        @Override
        public void onReceivedError(WebView view, int errorCode, String description, String failingUrl)
        {
            super.onReceivedError(view, errorCode, description, failingUrl);
            errorOccurred = true;
            Log.d(TAG, "onReceivedError()");
            viewPager.setCurrentItem(POSITION_ERROR_FRAGMENT,false);
        }
    }


}