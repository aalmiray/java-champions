= Map
Frank Delporte
:jbake-type: page
:jbake-status: published
:linkattrs:

++++
  <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
  <link rel="stylesheet" href="https://unpkg.com/leaflet.markercluster@1.4.1/dist/MarkerCluster.css" />
  <link rel="stylesheet" href="https://unpkg.com/leaflet.markercluster@1.4.1/dist/MarkerCluster.Default.css" />

  <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
  <script src="https://unpkg.com/leaflet.markercluster@1.4.1/dist/leaflet.markercluster.js"></script>

  <div id="map" style="height: 800px;"></div>

  <script>
      var data = [@LOCATIONS@];

      document.addEventListener('DOMContentLoaded', function() {
        var map = L.map('map').setView([0, 0], 2);

        // Add a tile layer (OpenStreetMap)
        L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
          attribution: '© OpenStreetMap contributors'
        }).addTo(map);

        var markers = L.markerClusterGroup({
          maxClusterRadius: 50, // Pixels to cluster markers
          disableClusteringAtZoom: 10, // Don't cluster when zoomed in past this level
          spiderfyOnMaxZoom: false // Spread out markers instead of zooming
        });
        data.forEach(champion => {
          var marker = L.marker([champion.lat, champion.lng], {
            title: champion.name,
            alt: champion.name
          });
          marker.bindPopup(champion.name);
          markers.addLayer(marker);
        });
        map.addLayer(markers);
      });
  </script>
++++
