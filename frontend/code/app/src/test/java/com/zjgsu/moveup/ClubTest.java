package com.zjgsu.moveup;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 27)
public class ClubTest {

    @Test
    public void testDefaultConstructor() {
        Club club = new Club();

        assertNotNull(club);
        assertNull(club.id);
        assertNull(club.name);
        assertNull(club.location);
        assertEquals(0, club.imageResId);
        assertNull(club.imageUrl);
        assertNull(club.flag);
    }

    @Test
    public void testLocalImageConstructor() {
        Club club = new Club("001", "MoveUp Runners", "Hangzhou", R.drawable.term1, "🇨🇳");

        assertEquals("001", club.id);
        assertEquals("MoveUp Runners", club.name);
        assertEquals("Hangzhou", club.location);
        assertEquals(R.drawable.term1, club.imageResId);
        assertEquals("🇨🇳", club.flag);
        assertNull(club.imageUrl);
    }

    @Test
    public void testNetworkImageConstructor() {
        Club club = new Club("002", "City Sprinters", "Beijing",
                "https://example.com/avatar.jpg", "🇨🇳");

        assertEquals("002", club.id);
        assertEquals("City Sprinters", club.name);
        assertEquals("Beijing", club.location);
        assertEquals("https://example.com/avatar.jpg", club.imageUrl);
        assertEquals("🇨🇳", club.flag);
        assertEquals(0, club.imageResId);
    }

    @Test
    public void testFieldModification() {
        Club club = new Club();
        club.id = "003";
        club.name = "Test";
        club.location = "Shanghai";
        club.imageUrl = "https://img.example.com/1.jpg";
        club.imageResId = 42;
        club.flag = "🏴";

        assertEquals("003", club.id);
        assertEquals("Test", club.name);
        assertEquals("Shanghai", club.location);
        assertEquals("https://img.example.com/1.jpg", club.imageUrl);
        assertEquals(42, club.imageResId);
        assertEquals("🏴", club.flag);
    }

    @Test
    public void testConstructor_EmptyStrings() {
        Club club = new Club("", "", "", 0, "");

        assertEquals("", club.id);
        assertEquals("", club.name);
        assertEquals("", club.location);
        assertEquals(0, club.imageResId);
        assertEquals("", club.flag);
    }

    @Test
    public void testNetworkConstructor_EmptyFields() {
        Club club = new Club("", "", "", "", "");

        assertEquals("", club.id);
        assertEquals("", club.name);
        assertEquals("", club.location);
        assertEquals("", club.imageUrl);
        assertEquals("", club.flag);
    }

    @Test
    public void testNetworkConstructor_NullImageUrl() {
        Club club = new Club("004", "Club Null", "Nowhere", null, "🏳️");

        assertEquals("004", club.id);
        assertEquals("Club Null", club.name);
        assertEquals("Nowhere", club.location);
        assertNull(club.imageUrl);
        assertEquals("🏳️", club.flag);
    }
}
