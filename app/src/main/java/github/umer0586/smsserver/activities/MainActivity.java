package github.umer0586.smsserver.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import android.os.Bundle;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

import github.umer0586.smsserver.R;
import github.umer0586.smsserver.fragments.ServerFragment;
import github.umer0586.smsserver.fragments.SettingsFragment;

public class MainActivity extends AppCompatActivity {

    private ViewPager2 viewPager;
    private MyFragmentStateAdapter myFragmentStateAdapter;
    private TabLayout tabLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewPager = findViewById(R.id.viewpager);
        tabLayout = findViewById(R.id.tab_layout);


        myFragmentStateAdapter =  new MyFragmentStateAdapter(this);
        viewPager.setAdapter(myFragmentStateAdapter);

        new TabLayoutMediator(tabLayout,viewPager, (tab,position)->{

            if(position == 0)
                tab.setText("Server");
            else if(position == 1)
                tab.setText("Settings");

        }).attach();

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
                case 0:
                    return new ServerFragment();
                case 1:
                    return new SettingsFragment();
            }

            return null;
        }

        @Override
        public int getItemCount()
        {
            return 2;
        }
    }
}