package ethiopia.covid.android.network;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.UiThread;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ethiopia.covid.android.data.Case;
import ethiopia.covid.android.data.CovidStatItem;
import ethiopia.covid.android.data.Patients;
import ethiopia.covid.android.data.StatRecyclerItem;
import ethiopia.covid.android.data.WorldCovid;
import okhttp3.OkHttpClient;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/**
 * Created by BrookMG on 3/25/2020 in ethiopia.covid.android.network
 * inside the project CoVidEt .
 */
public class API {
    private PMOCovidAPI pmoCovidAPI;
    private WorldCovidAPI worldCovidAPI;
    private static ExecutorService executors;

    public API() {
        OkHttpClient okHttpClient = new OkHttpClient.Builder().build();

        Retrofit retrofit = new Retrofit.Builder().baseUrl("https://api.pmo.gov.et/v1/")
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        Retrofit retrofitWorld = new Retrofit.Builder().baseUrl("https://www.bing.com/covid/")
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        pmoCovidAPI = retrofit.create(PMOCovidAPI.class);
        worldCovidAPI = retrofitWorld.create(WorldCovidAPI.class);
        executors = Executors.newFixedThreadPool(5);
    }

    public interface OnItemReady<T> { void onItem(T item, String err); }

    @UiThread
    public void getCases(OnItemReady<Case> onCaseReady) {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        executors.execute(() -> {
            try {
                Response<List<Case>> response = pmoCovidAPI.getCases().execute();
                if (response.body() != null && response.body().size() > 0) {
                    mainHandler.post(() -> onCaseReady.onItem(response.body().get(0), ""));
                } else {
                    mainHandler.post(() -> {
                        try {
                            onCaseReady.onItem(null, response.errorBody() != null ? response.errorBody().string() : "Unknown error");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                }
            } catch (IOException e) {
                mainHandler.post(() -> onCaseReady.onItem(null , e.toString()));
                e.printStackTrace();
            }
        });
    }

    @UiThread
    public void getPatients(OnItemReady<Patients> onCaseReady) {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        executors.execute(() -> {
            try {
                Response<Patients> response = pmoCovidAPI.getPatients().execute();
                if (response.body() != null && response.body().getResults().size() > 0) {
                    mainHandler.post(() -> onCaseReady.onItem(response.body(), ""));
                } else {
                    mainHandler.post(() -> {
                        try {
                            onCaseReady.onItem(null, response.errorBody() != null ? response.errorBody().string() : "Unknown error");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                }
            } catch (IOException e) {
                mainHandler.post(() -> onCaseReady.onItem(null , e.toString()));
                e.printStackTrace();
            }
        });
    }
    
    @UiThread
    public void getWorldStat(OnItemReady<WorldCovid> onItemReady) {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        executors.execute(() -> {
            try {
                Response<WorldCovid> response = worldCovidAPI.getListOfStat().execute();
                if (response.body() != null) {
                    mainHandler.post(() -> onItemReady.onItem(response.body(), ""));
                } else {
                    mainHandler.post(() -> {
                        try {
                            onItemReady.onItem(null, response.errorBody() != null ? response.errorBody().string() : "Unknown error");
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                }
            } catch (IOException e) {
                mainHandler.post(() -> onItemReady.onItem(null , e.toString()));
                e.printStackTrace();
            }
        });
    }

    @UiThread
    public void getStatRecyclerContents(OnItemReady<List<StatRecyclerItem>> onItemReady) {

        conjure(() -> {
            List<StatRecyclerItem> returnable = new ArrayList<>();

            try {
                Case caseItem = getPmoCovidAPI().getCases().execute().body().get(0);
                returnable.add(new StatRecyclerItem("Ethiopia" , caseItem.getTotal(), caseItem.getDeceased(), caseItem.getStable()));
            } catch (Exception ignored) {}

            try {
                Patients patients = getPmoCovidAPI().getPatients().execute().body();
                returnable.add(
                        new StatRecyclerItem(
                                Arrays.asList("ID" , "Name", "Location", "Age", "Gender" , "Nationality", "RecentTravel", "Status"),
                                1,
                                patients.getResults()
                        )
                );
            } catch (Exception ignored) {}

            try {
                WorldCovid worldStat = getWorldCovidAPI().getListOfStat().execute().body();
                List<CovidStatItem> worldStatItems = new ArrayList<>();
                for (WorldCovid location : worldStat.getAreas()) {
                    worldStatItems.add(
                            new CovidStatItem(
                                    location.getDisplayName().replace(" ", ""),
                                    location.getTotalConfirmed(),
                                    (location.getTotalConfirmed() - (location.getTotalDeaths() + location.getTotalRecovered())),
                                    location.getTotalDeaths(),
                                    location.getTotalRecovered(),
                                    0, 0, 0
                            )
                    );
                }

                returnable.add(new StatRecyclerItem(
                        0,
                        worldStatItems,
                        Arrays.asList("Country" , "Infected", "Active", "Death", "Recovered" , "Critical", "Minor", "Suspected"),
                        1
                ));
            } catch (Exception ignored) {}

            return returnable;
        }, (content, err) -> onItemReady.onItem(content, err != null ? err.toString() : ""));

    }

    public interface ConjureBackground<T> { T blockToRunInBackground(); }
    public interface ConjureForeground<T> { void blockToRunOnMainThread(T content, Throwable err); }

    public static <T> void conjure(ConjureBackground<T> onBackground, ConjureForeground<T> onForeground) {
        Handler mainHandler = new Handler(Looper.getMainLooper());
        executors.execute(() -> {
            try {
                T returnable = onBackground.blockToRunInBackground();
                mainHandler.post(() -> onForeground.blockToRunOnMainThread(returnable, null));
            } catch (Exception e) {
                mainHandler.post(() -> onForeground.blockToRunOnMainThread(null, e));
                e.printStackTrace();
            }
        });
    }

    public PMOCovidAPI getPmoCovidAPI() {
        return pmoCovidAPI;
    }

    public WorldCovidAPI getWorldCovidAPI() {
        return worldCovidAPI;
    }
}