package edu.gatech.team10.weshelter;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.SearchView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class ShelterListActivity extends AppCompatActivity {

    private RecyclerView mRecyclerView;
    private ShelterAdapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;
    private SearchView searchView;
    final private Model model = Model.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shelter_list);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fabMaps = (FloatingActionButton) findViewById(R.id.fab_shelter_list_maps);
        fabMaps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Context context = view.getContext();
                Intent intent = new Intent(context, MapsActivity.class);
                startActivity(intent);
            }
        });


        mLayoutManager = new LinearLayoutManager(this);
        mRecyclerView = (RecyclerView) findViewById(R.id.shelter_list);
        mRecyclerView.setLayoutManager(mLayoutManager);

        mAdapter = new ShelterAdapter(model.getShelters());
        mRecyclerView.setAdapter(mAdapter);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_shelter_list, menu);

        // Associate searchable configuration with the SearchView
        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setMaxWidth(Integer.MAX_VALUE);

        // listening to search query text change
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // filter recycler view when query submitted
                mAdapter.getFilter().filter(query);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String query) {
                // filter recycler view when text is changed
                mAdapter.getFilter().filter(query);
                return false;
            }
        });

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() != R.id.action_search) {
            finish();
        }
        return true;
    }

    // RecyclerView set up
    public class ShelterAdapter extends RecyclerView.Adapter<ShelterAdapter.ViewHolder> implements Filterable {
        private List<Shelter> mDataset;
        private List<Shelter> mDatasetFiltered;

        // ViewHolder set up
        public class ViewHolder extends RecyclerView.ViewHolder {
            final View mView;
            final TextView mNameView;
            final TextView mRestrictions;
            Shelter mShelter;

            /**
             * Constructs a ViewHolder template for Shelter data.
             * @param view current view
             */
            public ViewHolder(View view) {
                super(view);
                mView = view;
                mNameView = (TextView) view.findViewById(R.id.textView_content_shelter_name);
                mRestrictions = (TextView) view.findViewById(R.id.textView_content_shelter_demo);
            }

            @Override
            public String toString() {
                return super.toString() + " '" + mNameView.getText() + "'";
            }
        }

        /**
         * Converts data for usage in RecyclerView
         * @param myDataset raw data list
         */
        public ShelterAdapter(List<Shelter> myDataset) {
            mDataset = myDataset;
            mDatasetFiltered = myDataset;
        }

        // Create new views (invoked by the layout manager)
        @Override
        public ShelterAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                       int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.content_shelter_list, parent, false);
            return new ViewHolder(view);
        }

        // Replace the contents of a view (invoked by the layout manager)
        @Override
        public void onBindViewHolder(final ViewHolder holder, int position) {
            holder.mShelter = mDatasetFiltered.get(position);
            holder.mNameView.setText(mDatasetFiltered.get(position).getName());
            holder.mRestrictions.setText(mDatasetFiltered.get(position).getRestriction());
            holder.mView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Context context = v.getContext();
                    Intent intent = new Intent(context, ShelterDetailActivity.class);

                    model.setActiveShelter(holder.mShelter);

                    context.startActivity(intent);
                }
            });
        }

        // Return the size of your dataset (invoked by the layout manager)
        @Override
        public int getItemCount() {
            return mDatasetFiltered.size();
        }

        @Override
        public Filter getFilter() {
            return new Filter() {
                @Override
                protected FilterResults performFiltering(CharSequence charSequence) {
                    String charString = charSequence.toString();
                    if (charString.isEmpty()) {
                        mDatasetFiltered = mDataset;
                    } else {
                        List<Shelter> filteredList = new ArrayList<>();
                        charString = convertSearch(charString);
                        for (Shelter s : mDataset) {
                            if (charString.toLowerCase().equals("men")
                                    && s.getRestriction().toLowerCase().contains("men")) {
                                if (!s.getRestriction().toLowerCase().contains("omen")) {
                                    filteredList.add(s);
                                }
                            } else {
                                if (s.getName().toLowerCase().contains(charString.toLowerCase())
                                        || s.getRestriction().toLowerCase()
                                            .contains(charString.toLowerCase())) {
                                    filteredList.add(s);
                                }
                            }
                        }

                        mDatasetFiltered = filteredList;
                    }

                    FilterResults filterResults = new FilterResults();
                    filterResults.values = mDatasetFiltered;
                    return filterResults;
                }

                @SuppressWarnings("unchecked")
                @Override
                protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                    //noinspection unchecked
                    mDatasetFiltered = (ArrayList<Shelter>) filterResults.values;
                    model.setFilteredShelters(mDatasetFiltered);
                    mAdapter.notifyDataSetChanged();
                }
            };
        }
    }

    /**
     * Enables "fuzzy" searching by converting allowed synonyms into searchable terms
     *
     * @param keyword unconverted search input
     * @return String approved search term or unchanged keyword
     */
    public String convertSearch(String keyword) {
        if (keyword.equals("male")
                || keyword.equals("man")
                || keyword.equals("men")
                || keyword.equals("boy")
                || keyword.equals("boys")) {
            return "men";
        } else if (keyword.equals("female")
                || keyword.equals("woman")
                || keyword.equals("women")
                || keyword.equals("girl")
                || keyword.equals("girls")) {
            return "women";
        } else if (keyword.equals("kid")
                || keyword.equals("kids")
                || keyword.equals("child")
                || keyword.equals("children")) {
            return "child";
        } else if (keyword.equals("everyone")
                || keyword.equals("everybody")
                || keyword.equals("anybody")
                || keyword.equals("anyone")) {
            return "anyone";
        } else if (keyword.equals("teen")
                || keyword.equals("teenager")
                || keyword.equals("teenagers")
                || keyword.equals("teens")
                || keyword.equals("young adult")) {
            return "young adult";
        } else if (keyword.equals("family")
                || keyword.equals("families")) {
            return "families";
        } else if (keyword.equals("veteran")
                || keyword.equals("veterans")) {
            return "veteran";
        } else if (keyword.equals("baby")
                || keyword.equals("babies")
                || keyword.equals("infants")
                || keyword.equals("infant")
                || keyword.equals("newborn")
                || keyword.equals("newborns")) {
            return "newborn";
        } else {
            return keyword;
        }
    }

}
