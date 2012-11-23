package com.sunnykwong.omc;

import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import java.net.URL;
import java.util.HashMap;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.os.Build;
import android.text.format.Time;
import android.util.Log;

public class METARHandler {

	//public static final String URL_V4CIVIL = "http://www.7timer.com/v4/bin/civil.php?app=omc&output=json&tzshift=0&unit=metric&lang=en&ac=0";
	public static final String HOST_NOAAMETAR = "tgftp.nws.noaa.gov";
	public static final String PATH_NOAAMETAR = "/data/observations/metar/stations/";
	public static JSONObject jsonWeather;
	public static int MAXRADIUS = 100; //Maximum acceptable radius (in KM) for METAR reporting
	
	static public void updateCurrentConditions(final double latitude, final double longitude) {

			// Update weather from provider.
			Thread t = new Thread() {
				@Override
				public void run() {
	
					FTPClient ftp = new FTPClient();
					
					try {
						ftp.connect(HOST_NOAAMETAR,FTPClient.DEFAULT_PORT);
						int reply = ftp.getReplyCode();
						if(!FTPReply.isPositiveCompletion(reply)) {
							ftp.disconnect();
							if (OMC.DEBUG)
								Log.i(OMC.OMCSHORT + "NOAAMETAR", "FTP server refused connection.");

					     }
						ftp.login("ftp", "mozilla@example.com");
						ftp.enterLocalPassiveMode();
					} catch(IOException e) {
				        if(ftp.isConnected()) {
				          try {
				            ftp.disconnect();
				          } catch(IOException f) {
				            // do nothing
				          }
				        }
						if (OMC.DEBUG)
							Log.i(OMC.OMCSHORT + "NOAAMETAR", "Could not connect to server.");
				        e.printStackTrace();
				        				      }
						// Start building URL string.
						String sPath = PATH_NOAAMETAR;
						
						Time tNow = new Time();
						tNow.setToNow();
						
						final String[] sICAOs = OMC.findClosestICAOs(latitude, longitude, MAXRADIUS);
//   				 	v1.4.1:  Auto weather provider.
//							If no nearby airport, do not use METAR.
						if (sICAOs==null) {
							if (OMC.DEBUG)
								Log.i(OMC.OMCSHORT + "NOAAMETAR", "No METAR-reporting stations withing a " + MAXRADIUS + "km radius... using interpolated conditions.");
							return;
						}						
						if (OMC.DEBUG)
							Log.i(OMC.OMCSHORT + "NOAAMETAR", "Nearest airports are " + OMC.flattenString(sICAOs));
						
						// Get forecast JSON.
						if (OMC.DEBUG)
							Log.i(OMC.OMCSHORT + "NOAAMETAR", "Start Building JSON.");
						String result = null;
						
						for (String s: sICAOs) {
							try {
								String sFilePath = sPath+ s +".TXT";
								
								if (OMC.DEBUG) Log.i(OMC.OMCSHORT + "NOAAMETAR", "Trying: " + sFilePath);
								InputStream is = ftp.retrieveFileStream(sFilePath);
								if (is==null) {
									System.out.println( ftp.getReplyString());
									throw new IOException();
								}
								
								result = OMC.streamToString(is);
								is.close();
								ftp.disconnect();
								if (OMC.DEBUG) {
									Log.i(OMC.OMCSHORT + "NOAAMETAR", "Airport " + s + " returned good METAR.");
									Log.i(OMC.OMCSHORT + "NOAAMETAR", result);
								}
								
								Metar metar = Metar.parse(s, result);
								metar.debugPrint();
								
								// Update Current Conditions with METAR info.
								jsonWeather = new JSONObject(OMC.PREFS.getString("weather", "{}"));
								jsonWeather.put("temp_f", (int)(metar.tempF+0.5));
								jsonWeather.put("temp_c", metar.tempC);
								
								// Sanity Check - change current-day forecast to include current conditions
								JSONObject today = jsonWeather.getJSONArray("zzforecast_conditions").getJSONObject(0);
								if (metar.tempF > today.getDouble("high")) {
									today.put("high",(int)(metar.tempF+0.5));
									today.put("high_c", metar.tempC);
								}
								if (metar.tempF < today.getDouble("low")) {
									today.put("low",(int)(metar.tempF+0.5));
									today.put("low_c", metar.tempC);
								}
								
								jsonWeather.put("condition_raw", metar.condition);
								jsonWeather.put("condition_code", metar.OMCConditionCode);

								jsonWeather.put("humidity_raw", (int)(metar.relHumidity+0.5));
								String humidityString = OMC.RString("humiditycondition") +
										(int)(metar.relHumidity+0.5) + "%";
								jsonWeather.put("humidity", humidityString);

								jsonWeather.put("wind_speed_knots", metar.windSpdKT);
								jsonWeather.put("wind_speed_mph", metar.windSpdMPH);
								jsonWeather.put("wind_speed_mps", metar.windSpdMPS);
								jsonWeather.put("wind_direction",metar.windDirString);
								String windString = OMC.RString("windcondition") +
										metar.windDirString + " @ " +
										(int)(metar.windSpdMPH + 0.5) + " mph";
								jsonWeather.put("wind_condition", windString);
								
								//Mark current conditions as updated by METAR.
								jsonWeather.put("METAR", true);
								jsonWeather.put("ICAO", metar.icao);

								// In NOAA situations where current conditions are NA, fix that too
								JSONObject iTodaysWeatherForecastObject = jsonWeather.optJSONArray("zzforecast_conditions").getJSONObject(0);
								if (iTodaysWeatherForecastObject.getInt("condition_code")==0) {
									iTodaysWeatherForecastObject.put("condition_code",metar.OMCConditionCode);
									iTodaysWeatherForecastObject.put("condition", OMC.VERBOSEWEATHER[metar.OMCConditionCode]);
								}
								
								OMC.PREFS.edit().putString("weather", jsonWeather.toString(1)).commit();

								break;
							} catch (Exception e) {
								if (OMC.DEBUG)
									Log.i(OMC.OMCSHORT + "NOAAMETAR", "Airport " + s + " returned invalid METAR.");
								e.printStackTrace();
								continue;
							} 
							// Test cases
//							Metar.parse(null,"METAR EHAM 1050Z 24015KT 9000 RA SCT025 BKN040 10/09 Q1010 NOSIG").debugPrint();
//							Metar.parse(null,"METAR EGLL 0920Z 26005KT CAVOK 15/14 Q1013 NOSIG").debugPrint();
//							Metar.parse(null,"METAR EDDL 1550Z 26005KT 0550 R23L/0450 FZFG OVC002 M02/M02 Q0994 BECMG OVC005").debugPrint();
//							Metar.parse(null,"METAR EIDW 0900Z 24035G55KT 210V270 1700 +SHRA BKN007 OVC015CB 08/07 TEMPO 3500").debugPrint();
//							Metar.parse(null,"METAR LFPG 1250Z 28010KT 8000 HZ SCT070 BKN240 28/22 Q1003 NOSIG").debugPrint();
//							Metar.parse(null,"10Z KLIT 200953Z 00000KT 4SM -RA BR OVC030 07/05 A3013 RMK AO2 SLP205").debugPrint();
//							Metar.parse(null,"KJGG 071555Z AUTO 34011KT 10SM BKN017 OVC049 07/04 A2987 RMK AO1").debugPrint();
//							Metar.parse(null,"KQA7 071555Z 17004KT 9999 CLR 13/M07 A3016 RMK AO2A SLP209 T01331075").debugPrint();
//							Metar.parse(null,"MKJP 282000Z 290388KT 4000 -SHRA VCSH BKN014 FEW018CB BKN030 OVC100 27/22 Q0990").debugPrint();
//							Metar.parse(null,"METAR KJFK 242235Z 28024G36KT 7SM -RA BR BKN009 OVC020CB 26/24 A2998 RMK AO2 SLP993 T02640238 56012").debugPrint();
							

//							System.out.println(metar.getSkyCondition(0).getNaturalLanguageString());
//							System.out.println(metar.toString());
//							System.out.println(metar.getConditions().toString());
//							System.out.println(metar.getTemperature().getTemperature());
							
						}
//					} catch (Exception e) {
//						OMC.WEATHERREFRESHSTATUS = OMC.WRS_FAILURE;
//						e.printStackTrace();
//					}
				}
			};
			t.start();
		}
	}
