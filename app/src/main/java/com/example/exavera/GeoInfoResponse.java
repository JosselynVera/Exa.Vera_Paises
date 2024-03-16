package com.example.exavera;
import com.google.gson.annotations.SerializedName;
import java.util.List;

public class GeoInfoResponse {
    private String StatusMsg;

    private ResultsBean Results;

    private int StatusCode;

    public String getStatusMsg() {
        return StatusMsg;
    }

    public ResultsBean getResults() {
        return Results;
    }

    public int getStatusCode() {
        return StatusCode;
    }

    public static class ResultsBean {

        private String Name;
        private CapitalBean Capital;
        private CountryCodesBean CountryCodes;
        private String TelPref;
        private List<Double> GeoPt;
        private GeoRectangleBean GeoRectangle;

        public String getName() {
            return Name;
        }

        public CapitalBean getCapital() {
            return Capital;
        }

        public CountryCodesBean getCountryCodes() {
            return CountryCodes;
        }

        public String getTelPref() {
            return TelPref;
        }

        public List<Double> getGeoPt() {
            return GeoPt;
        }

        public GeoRectangleBean getGeoRectangle() {
            return GeoRectangle;
        }

        public static class CapitalBean {
            private String Name;

            public String getName() {
                return Name;
            }
        }

        public static class CountryCodesBean {
            private String iso2;
            private String iso3;
            private String fips;

            public String getIso2() {
                return iso2;
            }

            public String getIso3() {
                return iso3;
            }

            public String getFips() {
                return fips;
            }
        }

        public static class GeoRectangleBean {
            private double West;
            private double East;
            private double North;
            private double South;

            public double getWest() {
                return West;
            }

            public double getEast() {
                return East;
            }

            public double getNorth() {
                return North;
            }

            public double getSouth() {
                return South;
            }
        }
    }
}
