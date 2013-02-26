#/** ========================================================================= *
# * Copyright (C) 2011       IBM Corporation ( http://www.ibm.com/ )           *
# * Copyright (C) 2006, 2007 TAO Consulting Pte <http://www.taoconsulting.sg/> * 
# *                            All rights reserved.                            *
# * ========================================================================== *
# *                                                                            *
# * Licensed under the  Apache License, Version 2.0  (the "License").  You may *
# * not use this file except in compliance with the License.  You may obtain a *
# * copy of the License at <http://www.apache.org/licenses/LICENSE-2.0>.       *
# *                                                                            *
# * Unless  required  by applicable  law or  agreed  to  in writing,  software *
# * distributed under the License is distributed on an  "AS IS" BASIS, WITHOUT *
# * WARRANTIES OR  CONDITIONS OF ANY KIND, either express or implied.  See the *
# * License for the  specific language  governing permissions  and limitations *
# * under the License.                                                         *
# *                                                                            *
# * ========================================================================== */
!define PRODUCT_NAME "webDAVHelper"
!define MAIN_EXE_NAME "WebDocOpen.exe"
!define DEFAULT_PROTOCOL "webdav"
!define SECURE_PROTOCOL "webdavs"
!define PRODUCT_VERSION "1.3"
!define PRODUCT_PUBLISHER "IBM Corporation"
!define PRODUCT_PUBSHORT "IBM"
!define PRODUCT_WEB_SITE "http://www.ibm.com"
!define PRODUCT_DIR_REGKEY "Software\Microsoft\Windows\CurrentVersion\App Paths\${MAIN_EXE_NAME}"
!define PRODUCT_UNINST_KEY "Software\Microsoft\Windows\CurrentVersion\Uninstall\${PRODUCT_NAME}"
!define PRODUCT_UNINST_ROOT_KEY "HKLM"

Name "${PRODUCT_NAME} ${PRODUCT_VERSION}"
OutFile "WebDocOpenSetup.exe"
InstallDir "$PROGRAMFILES\${PRODUCT_NAME}"
Icon "${NSISDIR}\Contrib\Graphics\Icons\modern-install.ico"
UninstallIcon "${NSISDIR}\Contrib\Graphics\Icons\modern-uninstall.ico"
SilentInstall silent
SilentUninstall silent
InstallDirRegKey HKLM "${PRODUCT_DIR_REGKEY}" ""

Section "MainSection" SEC01
  SetOutPath "$INSTDIR"
  SetOverwrite ifnewer
  File "${MAIN_EXE_NAME}" #The main executable file
SectionEnd

Section "URLProtocol" SEC02 #make the helper act on the webdav protocol
  WriteRegStr HKCR "${DEFAULT_PROTOCOL}" "" "URL:WebDAV Helper Protocol"
  WriteRegStr HKCR "${DEFAULT_PROTOCOL}" "URL Protocol" ""
  WriteRegStr HKCR "${DEFAULT_PROTOCOL}\shell\open\command" "" "$INSTDIR\${MAIN_EXE_NAME} $\"%1$\""
  WriteRegStr HKCR "${SECURE_PROTOCOL}" "" "URL:WebDAV Helper Protocol"
  WriteRegStr HKCR "${SECURE_PROTOCOL}" "URL Protocol" ""
  WriteRegStr HKCR "${SECURE_PROTOCOL}\shell\open\command" "" "$INSTDIR\${MAIN_EXE_NAME} $\"%1$\""
  #Make webDAV be able to use basic auth
  WriteRegDWORD HKLM "STSTEM\CurrentControlSet\Services\WebClient\Parameters" "BasicAuthLevel" 0x00000002
  WriteRegDWORD HKLM "STSTEM\CurrentControlSet\Services\WebClient\Parameters" "UseBasicAuth"   0x00000002
SectionEnd

Section "ConfigEntries" SEC03
  #Here we record the Software installation
  WriteRegStr HKLM "Software\${PRODUCT_PUBSHORT}\${PRODUCT_NAME}" "CurrentVersion" "${PRODUCT_VERSION}"
  #Here we make Internet Explorer aware of the webDAVHelper, so we can read it from the user-agent in the HTTP header
  WriteRegStr HKLM "Software\Microsoft\Windows\CurrentVersion\Internet Settings\5.0\User Agent\Post Platform" "${PRODUCT_NAME}${PRODUCT_VERSION}" ""
SectionEnd

Section "MozillaEntries" SEC04
  #Here would be the entries to make Mozilla's Firefox 1.5++ aware of the webDAV Entries
  #We need to locate the profile of the current user and find the user.js --- add if missing
  #Need to go to  %AppData%\Mozilla\Firefox and check the profiles.ini where the profiles are
  #There we need to add / edit
  # user_pref("general.useragent.extra.webDAVHelper", "${PRODUCT_NAME}${PRODUCT_VERSION}");
SectionEnd

Section -Post
  WriteUninstaller "$INSTDIR\uninst.exe"
  WriteRegStr HKLM "${PRODUCT_DIR_REGKEY}" "" "$INSTDIR\${MAIN_EXE_NAME}"
  WriteRegStr ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}" "DisplayName" "$(^Name)"
  WriteRegStr ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}" "UninstallString" "$INSTDIR\uninst.exe"
  WriteRegStr ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}" "DisplayIcon" "$INSTDIR\${MAIN_EXE_NAME}"
  WriteRegStr ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}" "DisplayVersion" "${PRODUCT_VERSION}"
  WriteRegStr ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}" "URLInfoAbout" "${PRODUCT_WEB_SITE}"
  WriteRegStr ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}" "Publisher" "${PRODUCT_PUBLISHER}"
SectionEnd


Function un.onUninstSuccess
  MessageBox MB_ICONINFORMATION|MB_OK "$(^Name) was successfully removed from your computer."
FunctionEnd

Function un.onInit
  MessageBox MB_ICONQUESTION|MB_YESNO|MB_DEFBUTTON2 "Are you sure you want to completely remove $(^Name) and all of its components?" IDYES +2
  Abort
FunctionEnd

Section Uninstall
  Delete "$INSTDIR\${MAIN_EXE_NAME}"
  Delete "$INSTDIR\webDavHelper.txt"

  RMDir "$INSTDIR"

  DeleteRegKey ${PRODUCT_UNINST_ROOT_KEY} "${PRODUCT_UNINST_KEY}"
  DeleteRegKey HKLM "${PRODUCT_DIR_REGKEY}"
  DeleteRegKey HKLM "Software\${PRODUCT_PUBSHORT}\${PRODUCT_NAME}"
  DeleteRegKey HKCR "${DEFAULT_PROTOCOL}"
  DeleteRegKey HKCR "${SECURE_PROTOCOL}"
  DeleteRegValue HKLM "Software\Microsoft\Windows\CurrentVersion\Internet Settings\5.0\User Agent\Post Platform" "${PRODUCT_NAME}${PRODUCT_VERSION}"
  #Removal for Mozilla missing

  SetAutoClose true
SectionEnd
