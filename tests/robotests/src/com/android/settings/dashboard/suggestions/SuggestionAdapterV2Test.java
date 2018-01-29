/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.dashboard.suggestions;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.PendingIntent;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.service.settings.suggestions.Suggestion;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.android.internal.logging.nano.MetricsProto;
import com.android.settings.R;
import com.android.settings.SettingsActivity;
import com.android.settings.TestConfig;
import com.android.settings.dashboard.DashboardAdapterV2;
import com.android.settings.testutils.FakeFeatureFactory;
import com.android.settings.testutils.SettingsRobolectricTestRunner;
import com.android.settingslib.suggestions.SuggestionControllerMixin;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.List;

@RunWith(SettingsRobolectricTestRunner.class)
@Config(manifest = TestConfig.MANIFEST_PATH, sdk = TestConfig.SDK_VERSION)
public class SuggestionAdapterV2Test {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private SettingsActivity mActivity;
    @Mock
    private SuggestionControllerMixin mSuggestionControllerMixin;
    private FakeFeatureFactory mFeatureFactory;
    private Context mContext;
    private SuggestionAdapterV2 mSuggestionAdapter;
    private DashboardAdapterV2.DashboardItemHolder mSuggestionHolder;
    private List<Suggestion> mOneSuggestion;
    private List<Suggestion> mTwoSuggestions;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        mContext = RuntimeEnvironment.application;
        mFeatureFactory = FakeFeatureFactory.setupForTest();

        final Suggestion suggestion1 = new Suggestion.Builder("id1")
                .setTitle("Test suggestion 1")
                .build();
        final Suggestion suggestion2 = new Suggestion.Builder("id2")
                .setTitle("Test suggestion 2")
                .build();
        mOneSuggestion = new ArrayList<>();
        mOneSuggestion.add(suggestion1);
        mTwoSuggestions = new ArrayList<>();
        mTwoSuggestions.add(suggestion1);
        mTwoSuggestions.add(suggestion2);
    }

    @Test
    public void getItemCount_shouldReturnListSize() {
        mSuggestionAdapter = new SuggestionAdapterV2(mContext, mSuggestionControllerMixin,
                null /* savedInstanceState */, null /* callback */, null /* lifecycle */);
        mSuggestionAdapter.setSuggestions(mOneSuggestion);
        assertThat(mSuggestionAdapter.getItemCount()).isEqualTo(1);

        mSuggestionAdapter.setSuggestions(mTwoSuggestions);
        assertThat(mSuggestionAdapter.getItemCount()).isEqualTo(2);
    }

    @Test
    public void getItemViewType_shouldReturnSuggestionTile() {
        mSuggestionAdapter = new SuggestionAdapterV2(mContext, mSuggestionControllerMixin,
                null /* savedInstanceState */, null /* callback */, null /* lifecycle */);
        mSuggestionAdapter.setSuggestions(mOneSuggestion);
        assertThat(mSuggestionAdapter.getItemViewType(0))
                .isEqualTo(R.layout.suggestion_tile_v2);
    }

    @Test
    public void getItemType_hasButton_shouldReturnSuggestionWithButton() {
        final List<Suggestion> suggestions = new ArrayList<>();
        suggestions.add(new Suggestion.Builder("id")
                .setFlags(Suggestion.FLAG_HAS_BUTTON)
                .setTitle("123")
                .setSummary("456")
                .build());
        mSuggestionAdapter = new SuggestionAdapterV2(mContext, mSuggestionControllerMixin,
                null /* savedInstanceState */, null /* callback */, null /* lifecycle */);
        mSuggestionAdapter.setSuggestions(suggestions);

        assertThat(mSuggestionAdapter.getItemViewType(0))
                .isEqualTo(R.layout.suggestion_tile_with_button_v2);
    }

    @Test
    public void onBindViewHolder_shouldLog() {
        final View view = spy(LayoutInflater.from(mContext).inflate(
                R.layout.suggestion_tile, new LinearLayout(mContext), true));
        mSuggestionHolder = new DashboardAdapterV2.DashboardItemHolder(view);
        mSuggestionAdapter = new SuggestionAdapterV2(mContext, mSuggestionControllerMixin,
                null /* savedInstanceState */, null /* callback */, null /* lifecycle */);
        mSuggestionAdapter.setSuggestions(mOneSuggestion);

        // Bind twice
        mSuggestionAdapter.onBindViewHolder(mSuggestionHolder, 0);
        mSuggestionAdapter.onBindViewHolder(mSuggestionHolder, 0);

        // Log once
        verify(mFeatureFactory.metricsFeatureProvider).action(
                mContext, MetricsProto.MetricsEvent.ACTION_SHOW_SETTINGS_SUGGESTION,
                mOneSuggestion.get(0).getId());
    }

    @Test
    public void onBindViewHolder_itemViewShouldHandleClick()
            throws PendingIntent.CanceledException {
        final List<Suggestion> suggestions = makeSuggestions("pkg1");
        setupSuggestions(mActivity, suggestions);

        mSuggestionAdapter.onBindViewHolder(mSuggestionHolder, 0);
        mSuggestionHolder.itemView.performClick();

        verify(mSuggestionControllerMixin).launchSuggestion(suggestions.get(0));
        verify(suggestions.get(0).getPendingIntent()).send();
    }

    @Test
    public void onBindViewHolder_hasButton_buttonShouldHandleClick()
        throws PendingIntent.CanceledException {
        final List<Suggestion> suggestions = new ArrayList<>();
        final PendingIntent pendingIntent = mock(PendingIntent.class);
        suggestions.add(new Suggestion.Builder("id")
            .setFlags(Suggestion.FLAG_HAS_BUTTON)
            .setTitle("123")
            .setSummary("456")
            .setPendingIntent(pendingIntent)
            .build());
        mSuggestionAdapter = new SuggestionAdapterV2(mContext, mSuggestionControllerMixin,
            null /* savedInstanceState */, null /* callback */, null /* lifecycle */);
        mSuggestionAdapter.setSuggestions(suggestions);
        mSuggestionHolder = mSuggestionAdapter.onCreateViewHolder(
            new FrameLayout(RuntimeEnvironment.application),
            mSuggestionAdapter.getItemViewType(0));

        mSuggestionAdapter.onBindViewHolder(mSuggestionHolder, 0);
        mSuggestionHolder.itemView.findViewById(android.R.id.primary).performClick();

        verify(mSuggestionControllerMixin).launchSuggestion(suggestions.get(0));
        verify(pendingIntent).send();
    }

    @Test
    public void getSuggestions_shouldReturnSuggestionWhenMatch() {
        final List<Suggestion> suggestions = makeSuggestions("pkg1");
        setupSuggestions(mActivity, suggestions);

        assertThat(mSuggestionAdapter.getSuggestion(0)).isNotNull();
    }

    @Test
    public void onBindViewHolder_closeButtonShouldHandleClick()
        throws PendingIntent.CanceledException {
        final List<Suggestion> suggestions = makeSuggestions("pkg1");
        final SuggestionAdapterV2.Callback callback = mock(SuggestionAdapterV2.Callback.class);
        mSuggestionAdapter = new SuggestionAdapterV2(mActivity, mSuggestionControllerMixin,
            null /* savedInstanceState */, callback, null /* lifecycle */);
        mSuggestionAdapter.setSuggestions(suggestions);
        mSuggestionHolder = mSuggestionAdapter.onCreateViewHolder(
            new FrameLayout(RuntimeEnvironment.application),
            mSuggestionAdapter.getItemViewType(0));

        mSuggestionAdapter.onBindViewHolder(mSuggestionHolder, 0);
        mSuggestionHolder.itemView.findViewById(R.id.close_button).performClick();

        verify(callback).onSuggestionClosed(suggestions.get(0));
    }

    @Test
    public void onBindViewHolder_differentPackage_shouldNotTintIcon()
            throws PendingIntent.CanceledException {
        final Icon icon = mock(Icon.class);
        when(icon.getResPackage()).thenReturn("pkg1");
        when(mActivity.getPackageName()).thenReturn("pkg2");
        final Suggestion suggestion = new Suggestion.Builder("pkg1")
            .setPendingIntent(mock(PendingIntent.class))
            .setIcon(icon)
            .build();
        final List<Suggestion> suggestions = new ArrayList<>();
        suggestions.add(suggestion);
        mSuggestionAdapter = new SuggestionAdapterV2(mActivity, mSuggestionControllerMixin,
            null /* savedInstanceState */, null /* callback */, null /* lifecycle */);
        mSuggestionAdapter.setSuggestions(suggestions);
        mSuggestionHolder = mSuggestionAdapter.onCreateViewHolder(
            new FrameLayout(RuntimeEnvironment.application),
            mSuggestionAdapter.getItemViewType(0));
        DashboardAdapterV2.IconCache cache = mock(DashboardAdapterV2.IconCache.class);
        final Drawable drawable = mock(Drawable.class);
        when(cache.getIcon(icon)).thenReturn(drawable);
        ReflectionHelpers.setField(mSuggestionAdapter, "mCache", cache);

        mSuggestionAdapter.onBindViewHolder(mSuggestionHolder, 0);

        verify(drawable, never()).setTint(anyInt());
    }

    @Test
    public void onBindViewHolder_samePackage_shouldTintIcon()
            throws PendingIntent.CanceledException {
        final Icon icon = mock(Icon.class);
        final String packageName = "pkg1";
        when(icon.getResPackage()).thenReturn(packageName);
        when(mActivity.getPackageName()).thenReturn(packageName);
        final Suggestion suggestion = new Suggestion.Builder(packageName)
            .setPendingIntent(mock(PendingIntent.class))
            .setIcon(icon)
            .build();
        final List<Suggestion> suggestions = new ArrayList<>();
        suggestions.add(suggestion);
        mSuggestionAdapter = new SuggestionAdapterV2(mActivity, mSuggestionControllerMixin,
            null /* savedInstanceState */, null /* callback */, null /* lifecycle */);
        mSuggestionAdapter.setSuggestions(suggestions);
        mSuggestionHolder = mSuggestionAdapter.onCreateViewHolder(
            new FrameLayout(RuntimeEnvironment.application),
            mSuggestionAdapter.getItemViewType(0));
        DashboardAdapterV2.IconCache cache = mock(DashboardAdapterV2.IconCache.class);
        final Drawable drawable = mock(Drawable.class);
        when(cache.getIcon(icon)).thenReturn(drawable);
        ReflectionHelpers.setField(mSuggestionAdapter, "mCache", cache);
        TypedArray typedArray = mock(TypedArray.class);
        final int colorAccent = 1234;
        when(mActivity.obtainStyledAttributes(any())).thenReturn(typedArray);
        when(typedArray.getColor(anyInt(), anyInt())).thenReturn(colorAccent);

        mSuggestionAdapter.onBindViewHolder(mSuggestionHolder, 0);

        verify(drawable).setTint(colorAccent);
    }

    private void setupSuggestions(Context context, List<Suggestion> suggestions) {
        mSuggestionAdapter = new SuggestionAdapterV2(context, mSuggestionControllerMixin,
                null /* savedInstanceState */, null /* callback */, null /* lifecycle */);
        mSuggestionAdapter.setSuggestions(suggestions);
        mSuggestionHolder = mSuggestionAdapter.onCreateViewHolder(
                new FrameLayout(RuntimeEnvironment.application),
                mSuggestionAdapter.getItemViewType(0));
    }

    private List<Suggestion> makeSuggestions(String... pkgNames) {
        final List<Suggestion> suggestions = new ArrayList<>();
        for (String pkgName : pkgNames) {
            final Suggestion suggestion = new Suggestion.Builder(pkgName)
                    .setPendingIntent(mock(PendingIntent.class))
                    .build();
            suggestions.add(suggestion);
        }
        return suggestions;
    }
}
