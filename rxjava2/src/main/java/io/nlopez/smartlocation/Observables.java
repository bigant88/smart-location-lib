package io.nlopez.smartlocation;

import android.content.Context;
import android.location.Location;
import android.support.annotation.NonNull;

import java.util.List;

import io.nlopez.smartlocation.geocoding.GeocodingUpdatedListener;
import io.nlopez.smartlocation.geocoding.ReverseGeocodingUpdatedListener;
import io.nlopez.smartlocation.geocoding.common.LocationAddress;
import io.nlopez.smartlocation.location.LocationUpdatedListener;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.Single;
import io.reactivex.SingleEmitter;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.functions.Action;
import io.reactivex.subjects.SingleSubject;

/**
 * Creates RxJava Observables for all the library calls.
 */
public final class Observables {
    private Observables() {
        throw new AssertionError("This should not be instantiated");
    }

    /**
     * Returns a RxJava Observable for Location changes
     *
     * @param locationBuilder instance with the needed configuration
     * @return Observable for Location changes
     */
    @NonNull
    public static Observable<Location> from(@NonNull final SmartLocation.LocationBuilder locationBuilder) {
        return Observable.create(new ObservableOnSubscribe<Location>() {
            @Override
            public void subscribe(final ObservableEmitter<Location> emitter) throws Exception {
                locationBuilder.start(new LocationUpdatedListener.SimpleLocationUpdatedListener() {
                    @Override
                    public void onLocationUpdated(Location location) {
                        emitter.onNext(location);
                    }

                    @Override
                    public void onAllProvidersFailed() {
                        emitter.onError(new RuntimeException("All providers failed"));
                    }
                });
            }
        }).doOnDispose(new Action() {
            @Override
            public void run() throws Exception {
                locationBuilder.stop();
            }
        });
    }

    /**
     * Returns a RxJava single for direct geocoding results, aka get a Location from an address or name of a place.
     *
     * @param context    caller context
     * @param address    address or name of the place we want to get the location of
     * @param maxResults max number of coincidences to return
     * @return Single for results. Gets a terminal event after the response.
     */
    @NonNull
    public static Single<List<LocationAddress>> fromAddress(@NonNull final Context context, @NonNull final String address, final int maxResults) {
        return SingleSubject.create(new SingleOnSubscribe<List<LocationAddress>>() {
            @Override
            public void subscribe(final SingleEmitter<List<LocationAddress>> emitter) {
                SmartLocation.with(context)
                        .geocoding()
                        .maxResults(maxResults)
                        .findLocationByName(address, new GeocodingUpdatedListener.SimpleGeocodingUpdatedListener() {
                            @Override
                            public void onLocationResolved(String name, List<LocationAddress> results) {
                                emitter.onSuccess(results);
                            }
                        });

            }
        });
    }

    /**
     * Returns a RxJava single for reverse geocoding results, aka get an address from a Location.
     *
     * @param context    caller context
     * @param location   location we want to know the address od
     * @param maxResults max number of coincidences to return
     * @return Single for results. Gets a terminal event after the response
     */
    @NonNull
    public static Single<List<LocationAddress>> fromLocation(@NonNull final Context context, @NonNull final Location location, final int maxResults) {
        return SingleSubject.create(new SingleOnSubscribe<List<LocationAddress>>() {
            @Override
            public void subscribe(final SingleEmitter<List<LocationAddress>> emitter) {
                SmartLocation.with(context)
                        .geocoding()
                        .maxResults(maxResults)
                        .findNameByLocation(location,
                                new ReverseGeocodingUpdatedListener.SimpleReverseGeocodingUpdatedListener() {
                                    @Override
                                    public void onAddressResolved(Location original, List<LocationAddress> results) {
                                        emitter.onSuccess(results);
                                    }
                                });
            }
        });
    }
}
