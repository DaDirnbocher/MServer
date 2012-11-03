/*
 * MediathekView
 * Copyright (C) 2008 W. Xaver
 * W.Xaver[at]googlemail.com
 * http://zdfmediathk.sourceforge.net/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package mediathekServer.update;

import mediathekServer.tool.MS_Daten;
import mediathekServer.tool.MS_Funktionen;
import mediathekServer.tool.MS_Log;

public class MS_Update {

    public static boolean updaten() {
        boolean ret = true;
        // nach Update suchen
        String updateUrl = MS_UpdateSuchen.checkVersion();
        if (updateUrl.equals("")) {
            MS_Log.systemMeldung("Programm noch aktuell");
        } else {
            String jarPfad = MS_Funktionen.getPathJar();
            ret = MS_Updaten.updaten(updateUrl, jarPfad, MS_Daten.getUserAgent());
            if (ret) {
                MS_Log.systemMeldung("Programmupdate OK");
            }
        }
        return ret;
    }
}
