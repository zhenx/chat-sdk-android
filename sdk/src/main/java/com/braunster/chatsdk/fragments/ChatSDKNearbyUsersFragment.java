package com.braunster.chatsdk.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ProgressBar;

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

    public static boolean ASC = true;
    public static boolean DESC = false;

    private ListView listNearbyUsers;
    private ChatSDKUsersListAdapter adapter;
    private ProgressBar progressBar;
    private UIUpdater uiUpdater;

    private GeoLocation currentUserGeoLocation = new GeoLocation(0.0, 0.0);
    private Map<BUser, GeoLocation> usersLocationsMap;

    public static ChatSDKNearbyUsersFragment newInstance() {
        return new ChatSDKNearbyUsersFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(false);

        usersLocationsMap = new HashMap<BUser, GeoLocation>();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        init(inflater);

        loadDataOnBackground();

        return mainView;
    }

    private void init(LayoutInflater inflater){
        mainView = inflater.inflate(R.layout.chat_sdk_activity_nearby_users, null);

        // Register this view with the GeoFireManager
        BNetworkManager.sharedManager().getNetworkAdapter().getGeoFireManager().setGeoDelegate(this);
        BNetworkManager.sharedManager().getNetworkAdapter().getGeoFireManager().start();

        initViews();
    }

    @Override
    public void initViews() {
        listNearbyUsers = (ListView) mainView.findViewById(R.id.list_nearby_users);
        progressBar = (ProgressBar) mainView.findViewById(R.id.chat_sdk_progress_bar);
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
        if(adapter != null) {
            adapter.clear();

            for(BUser user : userDistanceMap.keySet()) {
                adapter.addRow(user, userDistanceMap.get(user));
            }
        }
    }

    public boolean userAdded(BUser user, GeoLocation location) {
        usersLocationsMap.put(user, location);
        updateList(getSortedUsersDistanceMap());

        Timber.v("user added: " + user.getEntityID());

        return true;
    }

    public boolean userMoved(BUser user, GeoLocation location) {
        usersLocationsMap.put(user, location);
        updateList(getSortedUsersDistanceMap());

        Timber.v("user moved: " + user.getEntityID());

        return true;
    }

    public boolean userRemoved(BUser user) {
        usersLocationsMap.remove(user);
        updateList(getSortedUsersDistanceMap());

        Timber.v("user removed: " + user.getEntityID());

        return true;
    }

    public boolean setCurrentUserGeoLocation(GeoLocation location) {
        currentUserGeoLocation = location;
        updateList(getSortedUsersDistanceMap());

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
