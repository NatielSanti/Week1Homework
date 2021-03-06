package ru.natiel.week1homework.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.*;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import io.reactivex.SingleObserver;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import ru.natiel.week1homework.*;
import ru.natiel.week1homework.api.Api;
import ru.natiel.week1homework.api.WebService;
import ru.natiel.week1homework.model.AuthResponse;
import ru.natiel.week1homework.model.ChargeModel;
import ru.natiel.week1homework.model.ItemRemote;
import ru.natiel.week1homework.screen.MainActivity;
import ru.natiel.week1homework.screen.SecondActivity;
import ru.natiel.week1homework.screen.adapter.ChargeAdapter;
import ru.natiel.week1homework.screen.adapter.ChargeModelAdapterListener;

import java.util.ArrayList;
import java.util.List;

public class BudgetFragment extends Fragment implements ChargeModelAdapterListener, ActionMode.Callback, FragmentInterface {

    public static final int REQUEST_CODE = 100;
    private final static String COLOR_ID = "colorId";
    private final static String TYPE = "fragmentType";
    private List<Disposable> disposables = new ArrayList<>();
    private int color;
    private String type;
    private View view;
    private ChargeAdapter adapter;
    private SwipeRefreshLayout swipeRefreshLayout;
    private ActionMode actionMode;
    private WebService webService;
    private Api api;
    private String authToken;

    public static BudgetFragment newInstance(final int colorId, final String type) {
        BudgetFragment budgetFragment = new BudgetFragment();
        budgetFragment.color = colorId;
        budgetFragment.type = type;
        Bundle bundle = new Bundle();
        bundle.putInt(COLOR_ID, colorId);
        bundle.putString(TYPE, type);
        budgetFragment.setArguments(bundle);
        return budgetFragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        webService = WebService.getInstance();
        api = webService.getApi();
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull final LayoutInflater inflater,
            @Nullable final ViewGroup container,
            @Nullable final Bundle savedInstanceState
    ) {
        view = inflater.inflate(R.layout.fragment_budget, null);

        RecyclerView recyclerView = view.findViewById(R.id.budget_item_list);
        swipeRefreshLayout = view.findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                loadItems();
            }
        });
        adapter = new ChargeAdapter();
        adapter.setListener(this);
        recyclerView.setAdapter(adapter);

        final FloatingActionButton fab = view.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final int activeFragmentIndex = MainActivity.viewPager.getCurrentItem();
                Fragment activeFragment = getActivity().getSupportFragmentManager().getFragments().get(activeFragmentIndex);
                Intent intent = new Intent(getActivity(), SecondActivity.class);
                intent.putExtra("fragmentId", activeFragmentIndex);
                activeFragment.startActivityForResult(intent, BudgetFragment.REQUEST_CODE);
            }
        });

        loadItems();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadItems();
    }

    @Override
    public void onDestroy() {
        for (Disposable disposable : disposables) {
            disposable.dispose();
        }
        super.onDestroy();
    }

    @Override
    public void loadItems() {
        getToken();
        api.request(type, authToken)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<List<ItemRemote>>() {
                    @Override
                    public void onSubscribe(Disposable d) {}

                    @Override
                    public void onSuccess(List<ItemRemote> itemRemotes) {
                        swipeRefreshLayout.setRefreshing(false);
                        List<ChargeModel> chargeModels = new ArrayList<>(itemRemotes.size());
                        int total = 0;
                        for (ItemRemote item : itemRemotes) {
                            ChargeModel chargeModel = new ChargeModel(item);
                            chargeModels.add(chargeModel);
                            total += item.getPrice();
                        }
                        SharedPreferences sharedPreferences = getActivity().getSharedPreferences(
                                getString(R.string.app_name), Context.MODE_PRIVATE);
                        sharedPreferences.edit().putInt(type, total).apply();
                        adapter.setNewData(chargeModels);
                    }

                    @Override
                    public void onError(Throwable e) {
                        swipeRefreshLayout.setRefreshing(false);
                        Toast.makeText(view.getContext().getApplicationContext(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

    }

    @Override
    public void onItemClick(ChargeModel item, int position) {
        adapter.clearItems(position);
        if (actionMode != null) {
            actionMode.setTitle(getString(R.string.selected, String.valueOf(adapter.getSelectedSize())));
        }
    }

    @Override
    public void onItemLongClick(ChargeModel item, int position) {
        if (actionMode == null && getActivity() != null)
            getActivity().startActionMode(this);
        adapter.toogleItem(position);
        if (actionMode != null) {
            actionMode.setTitle(getString(R.string.selected, String.valueOf(adapter.getSelectedSize())));
        }
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        this.actionMode = mode;
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        MenuInflater menuInflater = new MenuInflater(getActivity());
        menuInflater.inflate(R.menu.menu_delete, menu);
        return true;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        if (item.getItemId() == R.id.remove) {
            new AlertDialog.Builder(getContext())
                    .setMessage(R.string.confirm)
                    .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            removeItems();
                        }
                    })
                    .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Toast.makeText(getActivity(), getString(R.string.fail_remove), Toast.LENGTH_SHORT).show();
                        }
                    }).show();
        }
        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        this.actionMode = null;
        adapter.clearSelected();
    }

    private void removeItems() {
        List<Integer> selecteditems = adapter.getSelectedItemIdList();
        getToken();
        for (Integer id : selecteditems) {
            api.remove(id, authToken)
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new SingleObserver<Object>() {

                    @Override
                    public void onSubscribe(Disposable d) {}

                    @Override
                    public void onSuccess(Object o) {
                        loadItems();
                        adapter.clearSelected();
                    }

                    @Override
                    public void onError(Throwable e) {
                        Log.d("ERROR", e.getLocalizedMessage());
                    }
                });
        }
    }

    @Override
    public void getToken() {
        if (TextUtils.isEmpty(authToken)) {
            SharedPreferences sharedPreferences = view.getContext().getSharedPreferences(getString(R.string.app_name), Context.MODE_PRIVATE);
            authToken = sharedPreferences.getString(AuthResponse.AUTH_TOKEN_KEY, "");
        }
    }
}
