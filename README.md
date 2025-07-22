# Mayeo's GlyphMatrix Toys
Some toys to be used with the GlyphMatrix on the Nothing Phone 3. All still heavily beta, being worked on in my spare time.

This is the source code only, there is no final "app" yet. The repo must be cloned and opened in Android Studio, then built onto a Nothing Phone 3.

There is a lot of tidying up, and proper utilisation of the GlyphMatrix Developer Kit needed.

# Current Toys

## Angle Finder
Find the angle of a tilted surface by placing the phone on its edge (with the volume buttons on the table).

GlyphMatrix will show the angle, and a line matching the horizontal.

<img src="imgs/AngleGlyph_Flat.jpg" width="300" height="400" alt="AngleGlyph when (mostly) flat">
<img src="imgs/AngleGlyph_Tilt.jpg" width="300" height="400" alt="AngleGlyph when at an angle">

## REST API
Display info collected from a REST API. Currently built for use with a home automation setup to show temperatures from different rooms, but hoping to generalise it in future.

Room name scrolls across the screen, while the temperature remains static.

Utilises the Long Press on the Glyph button to cycle between rooms.

<img src="imgs/RestGlyph.jpg" width="300" height="400" alt="Rest Glyph showing Kitchen temperature">

Sample JSON structure this works with (second value, humidity, is not used):

```
{
  "Room1": [21, 65],
  "Room2": [19, 55],
  "Room3": [23, 70]
}
```

## Now Playing
Show the song title and artist for the currently playing song.

Text will scroll across the GlyphMatrix screen.

This supports AOD, but is definitely using things outside what was intended, and can lead to issues with the GlyphMatrix display.

# Other things

## DrawUtils

A utility object (that may be useful to others) that helps draw to the screen:
- Static, scrolling, and rotated text (using 3x5 characters)
- Line drawing
- More to be added


# References
Some help from the sample Toys in the [GlyphMatrix Developer Kit](https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit/tree/main), and a little ChatGPT
