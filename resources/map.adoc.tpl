= Map
Frank Delporte
:jbake-type: page
:jbake-status: published
:linkattrs:

++++
<div id="map"></div>
  <script src="https://unpkg.com/leaflet@1.7.1/dist/leaflet.js"></script>
  <script>
    // Initialize the map
    var map = L.map('map').setView([0, 0], 2);

    // Add a marker layer
    var markers = new L.LayerGroup();

    // Data
    var data = [@LOCATIONS@];

    // Visualize the locations
    data => {
        // Loop through each champion and add a marker to the map
        data.forEach(champion => {
          var marker = L.marker([champion.lat, champion.long], {
            title: champion.name,
            alt: champion.name
          });
          markers.addLayer(marker);
        });
        // Add the marker layer to the map
        map.addLayer(markers);
      };
  </script>
++++
