package com.example.rythm;

import android.content.Intent;
import android.graphics.Canvas;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import org.json.JSONException;
import org.json.JSONObject;

import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator;

public class PlayListFragment extends Fragment implements PlayListAdapter.onSongListener {

    private static final String TAG_FRAGMENT = "fragment";
    private List<Song> songs;
    private ImageView btnAddSong,
                      btnFilterSongs,
                      btnEditPlayList;
    private TextView tvPlayListName;
    private PlayListAdapter playListAdapter;
    private SearchAddSongFragment searchAddSongFragment;
    private FilterSongsFragment filterSongsFragment;
    private EditPlayListFragment editPlayListFragment;
    private String playListName;
    private RecyclerView recyclcerViewSongs;

    private RequestQueue queue;

    //Connection to Firestore
    private FirebaseFirestore db = FirebaseFirestore.getInstance();

    private String playlistId;

    public PlayListFragment() {
        // Required empty public constructor
    }

    public PlayListFragment(String playListName) {
        this.playListName = playListName;
    }

    public void setPlaylistId(String playlistId) {
        this.playlistId = playlistId;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_play_list, container, false);

        this.searchAddSongFragment = new SearchAddSongFragment(this.playlistId, this.playListName);
        this.filterSongsFragment = new FilterSongsFragment();
        this.editPlayListFragment = new EditPlayListFragment();

        this.btnAddSong = view.findViewById(R.id.btnAddSong);
        this.btnFilterSongs = view.findViewById(R.id.btnFilterSongs);
        this.btnEditPlayList = view.findViewById(R.id.btnEditPlaylist);
        this.tvPlayListName = view.findViewById(R.id.tvPlayListName);
        this.tvPlayListName.setText(this.playListName);
        this.btnAddSong.setOnClickListener(v -> {
            this.setFragment(this.searchAddSongFragment);
        });
        this.btnFilterSongs.setOnClickListener(v -> {
           this.setFragment(this.filterSongsFragment);
        });
        this.btnEditPlayList.setOnClickListener(v -> {
            this.setFragment(this.editPlayListFragment);
        });
        getSongsFromFirebase();

        this.songs = new ArrayList<>();

        this.playListAdapter = new PlayListAdapter(this.songs, view.getContext(), this);
        this.recyclcerViewSongs = view.findViewById(R.id.recyclerViewSongs);
        recyclcerViewSongs.setHasFixedSize(true);
        recyclcerViewSongs.setLayoutManager(new LinearLayoutManager(view.getContext()));
        new ItemTouchHelper(songsTouchHelper).attachToRecyclerView(this.recyclcerViewSongs);
        recyclcerViewSongs.setAdapter(this.playListAdapter);
        return view;
    }

    @Override
    public void onSongClick(int pos) {
        Intent i = new Intent(getContext(), SongView.class);
        i.putExtra("playlistPosition", pos);
        i.putExtra("playlistId", this.playlistId);
        i.putExtra("playlistName", playListName);

        //for (Song song : songs) Log.d("aiuda", "onSongClick: " + song.getDeezerTrackId());

        Log.d("aiuda", "onSongClick: playlist fragment" + songs.get(pos).getDeezerTrackId());
        startActivity(i);
    }

    private void getSongsFromFirebase() {
        Query songsQuery = db.collection("Songs")
                .whereEqualTo("playlistId", playlistId)
                .orderBy("addedTimestamp")
                .orderBy("deezerTrackId");
        songsQuery.addSnapshotListener((documentSnapshots, e) -> {
            if (documentSnapshots == null) return;
            for (DocumentChange doc: documentSnapshots.getDocumentChanges()){
                if (doc.getType() == DocumentChange.Type.ADDED){
                    String deezerTrackId = String.valueOf(doc.getDocument().get("deezerTrackId"));
                    Log.d("TOMATE", "getSongsFromFirebase: track id" + deezerTrackId);
                    this.playListAdapter.addSong(new Song());
                    fetchSongMetadata(deezerTrackId, playListAdapter.getItemCount()-1);
                }
            }
        });
    }

    private void fetchSongMetadata(String deezerTrackId, int pos) {
        if (!isAdded()) return; // Avoids exception
        this.queue = RequestController.getInstance(getContext()).getRequestQueue();

        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET,
                getString(R.string.track_endpoint_deezer_api) + deezerTrackId, null,
                track -> {
                    try {
                        String songName = track.getString("title");
                        JSONObject artist = track.getJSONObject("artist");
                        String artistName = artist.getString("name");
                        int duration = track.getInt("duration");
                        JSONObject album = track.getJSONObject("album");
                        String coverUrl = album.getString("cover");
                        this.playListAdapter.setSong(new Song(songName, artistName, duration, coverUrl, deezerTrackId), pos);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }, error -> Log.d("JSON", "onErrorResponse: " + error.getMessage()));

        queue.add(jsonObjectRequest);
    }

    public void setFragment(Fragment fragment) {
        FragmentManager mr = getParentFragmentManager();
        FragmentTransaction transaction = mr.beginTransaction();
        transaction.replace(R.id.container, fragment, TAG_FRAGMENT);
        transaction.commit();
    }

    ItemTouchHelper.SimpleCallback songsTouchHelper = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {

        private Song deletedSong;

        @Override
        public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
            return false;
        }

        @Override
        public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {

            final int position = viewHolder.getBindingAdapterPosition();

            switch (direction) {
                case ItemTouchHelper.LEFT:
                    this.deletedSong = songs.get(position);
                    songs.remove(position);
                    playListAdapter.notifyItemRemoved(position);
                    Snackbar.make(recyclcerViewSongs, this.deletedSong.getSongName(), Snackbar.LENGTH_LONG)
                            .setAction("Undo", v -> {
                                songs.add(position, deletedSong);
                                playListAdapter.notifyItemInserted(position);
                            }).show();
                    break;
            }
        }

        @Override
        public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
            new RecyclerViewSwipeDecorator.Builder(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                    .addSwipeLeftBackgroundColor(ContextCompat.getColor(getContext(), R.color.red))
                    .addSwipeLeftActionIcon(R.drawable.ic_delete_item)
                    .create()
                    .decorate();
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
        }
    };

}