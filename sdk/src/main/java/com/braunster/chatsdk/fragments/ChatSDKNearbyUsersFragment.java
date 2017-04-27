package com.braunster.chatsdk.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.braunster.chatsdk.R;
import com.braunster.chatsdk.Utils.Debug;
import com.braunster.chatsdk.adapter.ChatSDKUsersListAdapter;
import com.braunster.chatsdk.dao.BUser;
import com.braunster.chatsdk.dao.core.DaoCore;
import com.braunster.chatsdk.interfaces.GeoInterface;
import com.braunster.chatsdk.network.BNetworkManager;
import com.braunster.chatsdk.object.UIUpdater;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.util.GeoUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import timber.log.Timber;

/**
 * Created by Erk on 30.03.2016.
 */
public class ChatSDKNearbyUsersFragment extends ChatSDKBaseFragment implements GeoInterface {

    private static final String TAG = ChatSDKNearbyUsersFragment.class.getSimpleName();
    private static boolean DEBUG = Debug.NearbyUsersFragment;

    public static long delayInMillis = 500;

    public static boolean ASC = true;
    public static boolean DESC = false;

    private boolean showBands = true;
    private boolean isVisibleToUser;

    private ListView listNearbyUsers;
    private ChatSDKUsersListAdapter adapter;
    private ProgressBar progressBar;
    private UIUpdater uiUpdater;
    private TextView noUsersTextView;
    private ArrayList<Double> distanceBands;

    private GeoLocation currentUserGeoLocation = new GeoLocation(0.0, 0.0);
    private Map<BUser, GeoLocation> usersLocationsMap;

    public static ChatSDKNearbyUsersFragment newInstance() {
        return new ChatSDKNearbyUsersFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);

        // Populate the distance bands
        distanceBands = new ArrayList<Double>();
        distanceBands.add(0.0);
        distanceBands.add(1000.0);
        distanceBands.add(5000.0);
        distanceBands.add(10000.0);
        distanceBands.add(50000.0);

        usersLocationsMap = new HashMap<BUser, GeoLocation>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        init(inflater);

        loadDataOnBackground();

        return mainView;
    }

    @Override
    public void onResume() {
        super.onResume();

        // Re-register this view with the GeoFireManager
        BNetworkManager.sharedManager().getNetworkAdapter().getGeoFireManager().setGeoDelegate(this);
        BNetworkManager.sharedManager().getNetworkAdapter().getGeoFireManager().start(getActivity().getApplicationContext());

        if(isVisibleToUser)
        {
            updateList(getSortedUsersDistanceMap());
        }
    }

    private void init(LayoutInflater inflater){
        mainView = inflater.inflate(R.layout.chat_sdk_activity_nearby_users, null);

        initViews();

        // Register this view with the GeoFireManager
        BNetworkManager.sharedManager().getNetworkAdapter().getGeoFireManager().setGeoDelegate(this);
        BNetworkManager.sharedManager().getNetworkAdapter().getGeoFireManager().start(getActivity().getApplicationContext());
    }

    @Override
    public void initViews() {
        listNearbyUsers = (ListView) mainView.findViewById(R.id.list_nearby_users);
        progressBar = (ProgressBar) mainView.findViewById(R.id.chat_sdk_progress_bar);
        noUsersTextView = (TextView) mainView.findViewById(R.id.textView);

        initList();
    }

    private void initList(){
        adapter = new ChatSDKUsersListAdapter(getActivity());

        listNearbyUsers.setAdapter(adapter);

        listNearbyUsers.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (adapter.getItem(position).getType() == ChatSDKUsersListAdapter.TYPE_HEADER)
                    return;

                final BUser clickedUser = DaoCore.fetchEntityWithEntityID(BUser.class, adapter.getItem(position).getEntityID());

                createAndOpenThreadWithUsers(clickedUser.getMetaName(), clickedUser, getNetworkAdapter().currentUserModel());
            }
        });
    }

    public void updateList(Map<BUser, Double> userDistanceMap) {
        if(isVisibleToUser)
        {
            if (adapter != null) {
                adapter.clear();
            }

            // Set the display state to searching if the user location has not been aquired yet
            if(currentUserGeoLocation.latitude == 0.0 && currentUserGeoLocation.longitude == 0.0)
            {
                setState(R.string.searching_nearby_users);
            }
            else
            {
                // Set the display state to searching if there are no items in the locationsmap
                if (usersLocationsMap == null || usersLocationsMap.isEmpty())
                {
                    setState(R.string.no_nearby_users);
                }
                else
                {
                    setState(R.string.show_list);

                    if (adapter != null)
                    {
                        int currentBandIndex = 0;
                        Double currentDistance = 0.0;
                        String currentUserID = BNetworkManager.sharedManager().getNetworkAdapter().currentUserModel().getEntityID();

                        // Loop through all users and their distances
                        for (BUser user : userDistanceMap.keySet()) {
                            // Update the current distance
                            currentDistance = userDistanceMap.get(user);

                            if (showBands)
                            {
                                // Loop through all distance bands to see if we have to skip some of them
                                for (int i = currentBandIndex; i < distanceBands.size(); i++)
                                {
                                    // Check if we will still be in the bounds of the distanceBands array when incrementing and
                                    // check if the current distance is bigger than the lower border of the current band
                                    if ((currentBandIndex + 1) < distanceBands.size() && currentDistance >= distanceBands.get(currentBandIndex))
                                    {
                                        // If the current user distance exceeds the upper border of the current band go to the next band
                                        if (currentDistance > distanceBands.get(currentBandIndex + 1))
                                        {
                                            currentBandIndex++;
                                        }
                                        else
                                        {
                                            // If there is at least one user in a band add this band and increment the band index
                                            adapter.addBand(distanceBands.get(currentBandIndex), distanceBands.get(currentBandIndex + 1));
                                            currentBandIndex++;
                                        }
                                    }
                                }
                            }

                            if(!user.getEntityID().equals(currentUserID))
                            {
                                adapter.addRow(user, currentDistance);
                            }
                        }
                    }
                }
            }
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        this.isVisibleToUser = isVisibleToUser;

        if(isVisibleToUser)
        {
            // Show the "searching" text for a brief moment upon opening the fragment
            setState(R.string.searching_nearby_users);

            final Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    updateList(getSortedUsersDistanceMap());
                }
            }, delayInMillis);
        }
    }

    public boolean userAdded(BUser user, GeoLocation location) {
        usersLocationsMap.put(user, location);
        updateList(getSortedUsersDistanceMap());

        if(DEBUG) Timber.v("user added: " + user.getEntityID());

        return true;
    }

    public boolean userMoved(BUser user, GeoLocation location) {
        usersLocationsMap.put(user, location);
        updateList(getSortedUsersDistanceMap());

        if(DEBUG) Timber.v("user moved: " + user.getEntityID());

        return true;
    }

    public boolean userRemoved(BUser user) {
        usersLocationsMap.remove(user);
        updateList(getSortedUsersDistanceMap());

        if(DEBUG) Timber.v("user removed: " + user.getEntityID());

        return true;
    }

    public boolean setCurrentUserGeoLocation(GeoLocation location) {
        currentUserGeoLocation = location;
        updateList(getSortedUsersDistanceMap());

        return true;
    }

    public boolean setState(int stringResId) {
        if (noUsersTextView != null) {
            noUsersTextView.setText(getResources().getString(stringResId));
        }

        if(stringResId == R.string.show_list) {
            noUsersTextView.setVisibility(View.INVISIBLE);
            listNearbyUsers.setVisibility(View.VISIBLE);
        } else {
            listNearbyUsers.setVisibility(View.INVISIBLE);
            noUsersTextView.setVisibility(View.VISIBLE);
        }

        return true;
    }

    public Map<BUser, Double> getSortedUsersDistanceMap() {
        Map<BUser, Double> usersDistanceMap = getUsersDistanceMap();

        usersDistanceMap = sortByComparator(usersDistanceMap, ASC);

        return usersDistanceMap;
    }

    public Map<BUser, Double> getUsersDistanceMap() {

        Map<BUser, Double> usersDistanceMap = new HashMap<>();
        ConcurrentHashMap<BUser, GeoLocation> usersLocationMapLocal = new ConcurrentHashMap<BUser, GeoLocation>(usersLocationsMap);

        for(BUser user : usersLocationMapLocal.keySet()) {
            GeoLocation location = usersLocationMapLocal.get(user);

            Double distance = GeoUtils.distance(currentUserGeoLocation, location);

            usersDistanceMap.put(user, distance);
        }

        return usersDistanceMap;
    }

    private static Map<BUser, Double> sortByComparator(Map<BUser, Double> unsortMap, final boolean order)
    {
        List<Map.Entry<BUser, Double>> list = new LinkedList<Map.Entry<BUser, Double>>(unsortMap.entrySet());

        // Sorting the list based on values
        Collections.sort(list, new Comparator<Map.Entry<BUser, Double>>() {
            public int compare(Map.Entry<BUser, Double> o1,
                               Map.Entry<BUser, Double> o2) {
                if (order) {
                    return o1.getValue().compareTo(o2.getValue());
                } else {
                    return o2.getValue().compareTo(o1.getValue());

                }
            }
        });

        // Maintaining insertion order with the help of LinkedList
        Map<BUser, Double> sortedMap = new LinkedHashMap<BUser, Double>();
        for (Map.Entry<BUser, Double> entry : list)
        {
            sortedMap.put(entry.getKey(), entry.getValue());
        }

        return sortedMap;
    }
}
