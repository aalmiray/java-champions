= Stats
Andres Almiray
:jbake-type: page
:jbake-status: published
:linkattrs:

++++
<script type="text/javascript" src="https://www.gstatic.com/charts/loader.js"></script>
<script type="text/javascript">
google.charts.load('current', {packages: ['corechart', 'bar']});
google.charts.setOnLoadCallback(drawCharts);

function drawCharts() {
      var countriesData = google.visualization.arrayToDataTable([
        ['Country', 'Total'],
@COUNTRIES@
      ]);

      var countriesOptions = {
        chart: {
          title: 'Java Champions per country',
          subtitle: 'Reflects country of residence at time of nomination'
        },
        hAxis: {
          title: 'Total',
          minValue: 0,
        },
        vAxis: {
          title: 'Country'
        },
        bars: 'horizontal',
        height: '@COUNTRIES_HEIGHT@'
      };
      var countriesChart = new google.charts.Bar(document.getElementById('countries_chart_div'));
      countriesChart.draw(countriesData, countriesOptions);

      var yearsData = google.visualization.arrayToDataTable([
        ['YEAR', 'Total'],
@YEARS@
      ]);

      var yearsOptions = {
        chart: {
          title: 'Java Champions per year',
        },
        hAxis: {
          title: 'Total',
          minValue: 0,
        },
        vAxis: {
          title: 'Country'
        },
        bars: 'horizontal',
        height: '@YEARS_HEIGHT@'
      };
      var yearsChart = new google.charts.Bar(document.getElementById('years_chart_div'));
      yearsChart.draw(yearsData, yearsOptions);
    }
</script>
<div id="countries_chart_div"></div>

<br/><br/>

<div id="years_chart_div"></div>
++++
