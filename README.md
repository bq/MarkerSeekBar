# MarkerSeekBar
Custom Android SeekBar that adds a marker / thumb to display current value. Works on Android 4.4+

<img src="https://github.com/bq/MarkerSeekBar/raw/master/output.gif" width="350">

Usage
--------
Just include it in your xml as a replacement of your regular SeekBar. 
MarkerSeekBar is a direct subclass of AppCompatSeekBar, so you don't need to modify your existing code.

```xml
    <com.bq.markerseekbar.MarkerSeekBar
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:layout_marginBottom="100dp"
        android:max="100"
        app:markerColor="@color/colorPrimary"
        app:markerTextAppearance="@style/MarkerCustomTextStyle"
        app:markerTextColor="#ffffff" />
```

The complete attribute list:

Attribute                     | Description
------------------------------|-----------------------------------------
```showMarkerOnTouch```       | Automatic toggle. Default true.
```smoothTracking```          | Animate popup position. Default false.
```markerTextAppearance```    | The style of the text inside the marker.
```markerTextColor```         | The color of the text inside the marker.
```markerColor```             | The marker background color. Default accent color
```markerShadowRadius```      | The marker shadow radius. Use 0 to disable shadows. It affects marker size. Default 4dp.
```markerShadowColor```       | The marker shadow color. Default #331d1d1d.
```markerPopUpWindowSize```   | The popup size, its constant and measured to fit the longest possible text. Default 80dp.
```markerHorizontalOffset```  | Horizontal offset to align the marker tip and the progress thumb. Default (empirical) -8.5dp.
```markerVerticalOffset```    | Vertical offset to align the marker tip and the progress thumb. Default (empirical) -6dp.

If you want to customize another property open a PR or leave a comment!

Download
--------

Add [jitpack.io](https://jitpack.io/) to your repositories:
```groovy
allprojects {
    repositories { 
        maven { url "https://jitpack.io" }
    }
}
```

Include the dependency:
```groovy
compile "com.github.bq:markerseekbar:v0.9"
```
Enjoy!

License
-------
This project is licensed under the Apache Software License, Version 2.0.

    Copyright (c) 2016 bq

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
