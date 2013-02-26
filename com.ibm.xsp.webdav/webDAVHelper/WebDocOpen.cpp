/** ========================================================================= *
 * Copyright (C) 2011       IBM Corporation ( http://www.ibm.com/ )           *
 * Copyright (C) 2006, 2007 TAO Consulting Pte <http://www.taoconsulting.sg/> * 
 *                            All rights reserved.                            *
 * ========================================================================== *
 *                                                                            *
 * Licensed under the  Apache License, Version 2.0  (the "License").  You may *
 * not use this file except in compliance with the License.  You may obtain a *
 * copy of the License at <http://www.apache.org/licenses/LICENSE-2.0>.       *
 *                                                                            *
 * Unless  required  by applicable  law or  agreed  to  in writing,  software *
 * distributed under the License is distributed on an  "AS IS" BASIS, WITHOUT *
 * WARRANTIES OR  CONDITIONS OF ANY KIND, either express or implied.  See the *
 * License for the  specific language  governing permissions  and limitations *
 * under the License.                                                         *
 *                                                                            *
 * ========================================================================== */
#include <string>
#include <sstream>

#include <windows.h>

int main(int argc, char* argv[])
{
    const int MAX_KEY_NAME_LENGTH = 1024;

    const std::string HTTP    = "http://";
    const std::string HTTPS   = "https://";
    const std::string WEBDAV  = "webdav://";
    const std::string WEBDAVS = "webdavs://";

    if (argc == 2)
    {
        std::string fileName = argv[1];
        std::string ext = fileName.substr(fileName.rfind("."));

        if (fileName.substr(0, WEBDAV.length()) == WEBDAV)
        {
            fileName.replace(0, WEBDAV.length(), HTTP);
        }
        else if (fileName.substr(0, WEBDAVS.length()) == WEBDAVS)
        {
            fileName.replace(0, WEBDAVS.length(), HTTPS);
        }

        if (fileName.substr(0, HTTP.length())  == HTTP ||
            fileName.substr(0, HTTPS.length()) == HTTPS)
        {	
            HKEY hKeyExtension;

            LONG errorStatus = RegOpenKeyEx(HKEY_CLASSES_ROOT, TEXT(ext.c_str()), 0, KEY_READ, &hKeyExtension);

            if (errorStatus == ERROR_SUCCESS)
            {
                DWORD  type;
                LPBYTE pData = new BYTE[MAX_KEY_NAME_LENGTH];
                DWORD  dataLength = MAX_KEY_NAME_LENGTH;

                errorStatus = RegQueryValueExA(hKeyExtension, "", 0, &type, pData, &dataLength);

                if (errorStatus == ERROR_SUCCESS)
                {
                    HKEY hKeyOpenProgram;

                    std::string subKey = std::string(reinterpret_cast<LPCSTR>(pData)) + "\\shell\\open\\command";

                    errorStatus = RegOpenKeyEx(HKEY_CLASSES_ROOT, TEXT(subKey.c_str()), 0, KEY_READ, &hKeyOpenProgram);

                    if (errorStatus == ERROR_SUCCESS)
                    {
                        LPBYTE pData = new BYTE[MAX_KEY_NAME_LENGTH];
                        DWORD  dataLength = MAX_KEY_NAME_LENGTH;

                        errorStatus = RegQueryValueExA(hKeyOpenProgram, "", 0, &type, pData, &dataLength);

                        if (errorStatus == ERROR_SUCCESS)
					    {
                            std::string prg = reinterpret_cast<LPCSTR>(pData);
                            std::string cmd;

                            std::string::size_type prmPos = prg.rfind("%1");

                            cmd += (prmPos == std::string::npos ? (prg + " \"" + fileName + "\"")
                                                                : (prg.replace(prmPos, 2, fileName)));

                            WinExec(cmd.c_str(), 1);
                        }
                        else
                        {
                            std::cerr << "Run string for program not found." << std::endl;
                        }

                        delete [] pData;
                    }
                    else
                    {
                        std::cerr << "Opening program for this extension is not registered." << std::endl;
                    }
                }
                else
                {
                    std::cerr << "Opening program for this extension is not found." << std::endl;
                }

                delete [] pData;
            }
            else
            {
                std::cerr << "The file's extension is unknown." << std::endl;
            }
        }
        else
        {
            std::cerr << "This protocol does not supported." << std::endl;
        }
    }
    else
    {
        std::cout << "Usage: WebDocOpen.exe <FileName>" << std::endl;
    }

    return 0;
}

