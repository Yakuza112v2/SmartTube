package com.liskovsoft.smartyoutubetv2.common.app.models.playback.service;

import android.content.Context;

import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.playback.service.VideoStateService.State;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.util.Collections;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class PersonalHistoryDatabaseTest {
    private Context context;
    private PersonalHistoryDatabase database;

    @Before
    public void setUp() {
        context = RuntimeEnvironment.getApplication();
        context.deleteDatabase("personal_history.db");
        database = new PersonalHistoryDatabase(context);
    }

    @After
    public void tearDown() {
        database.close();
        context.deleteDatabase("personal_history.db");
    }

    @Test
    public void remembersOldVideosAfterCacheLimitAndDatabaseReopen() {
        State first = state("first-video", 42_000);
        database.save("tv", first);
        for (int i = 0; i < 350; i++) {
            database.save("tv", state("video-" + i, 20_000));
        }
        database.close();
        database = new PersonalHistoryDatabase(context);
        assertEquals(50, database.recent("tv", 50).size());
        State restored = database.find("tv", "first-video");
        assertNotNull(restored);
        assertEquals(42_000, restored.positionMs);
        assertEquals(first.timestamp, restored.timestamp);
    }

    @Test
    public void separatesProfilesAndUpdatesExistingProgress() {
        database.save("alice", state("same-video", 10_000));
        database.save("bob", state("same-video", 50_000));
        database.save("alice", state("same-video", 30_000));
        assertEquals(30_000, database.find("alice", "same-video").positionMs);
        assertEquals(50_000, database.find("bob", "same-video").positionMs);
        assertEquals(1, database.recent("alice", 300).size());
        database.remove("alice", "same-video");
        assertNull(database.find("alice", "same-video"));
        assertNotNull(database.find("bob", "same-video"));
    }

    @Test
    public void clearedHistoryDoesNotReappearFromLegacyPreferences() {
        State legacy = state("old-video", 60_000);
        database.importOnce("alice", Collections.singletonList(legacy));
        database.save("bob", legacy);
        database.clear("alice");
        database.close();
        database = new PersonalHistoryDatabase(context);
        database.importOnce("alice", Collections.singletonList(legacy));
        assertNull(database.find("alice", "old-video"));
        assertTrue(database.recent("alice", 300).isEmpty());
        assertNotNull(database.find("bob", "old-video"));
    }

    @Test
    public void deletionSurvivesReopeningAndRepeatedImport() {
        database.importOnce("tv", Collections.singletonList(state("deleted", 60_000)));
        database.remove("tv", "deleted");
        database.close();
        database = new PersonalHistoryDatabase(context);
        database.importOnce("tv", Collections.singletonList(state("deleted", 60_000)));
        assertNull(database.find("tv", "deleted"));
    }

    private State state(String id, long positionMs) {
        Video video = new Video();
        video.videoId = id;
        video.title = id;
        return new State(video, positionMs, 60_000, 1.25f);
    }
}
