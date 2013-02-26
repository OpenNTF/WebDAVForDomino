<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:dav="DAV:" xmlns:dxt="DXT:">
    <xsl:output method="html" indent="yes" doctype-public="-//W3C//DTD HTML 4.01 Transitional//EN"
        doctype-system="http://www.w3.org/TR/html4/loose.dtd"/>
    <xsl:template match="/">
        <html>
            <head>
                <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
                <title>Domino webDAV Repository list</title>

                <meta name="Description" content="List of repositories exposed through webDAV"/>
                <link rel="stylesheet" type="text/css" href="/oneuiv2.1/base/core.css"/>
                <link rel="stylesheet" type="text/css" href="/oneuiv2.1/defaultTheme/defaultTheme.css"/>
                <link rel="stylesheet" type="text/css"
                    href="/domjs/dojo-1.4.3/dijit/themes/tundra/tundra.css"/>
                <link rel="stylesheet" type="text/css"
                    href="/domjs/dojo-1.4.3/ibm/domino/widget/layout/css/domino-default.css"/>
                <link rel="stylesheet" type="text/css" href="/domjava/xsp/theme/oneuiv2.1/xsp.css"/>
                <link rel="stylesheet" type="text/css" href="/domjava/xsp/theme/oneuiv2.1/xspLTR.css"/>
                <!--[if IE 6]>
                                            <script type="text/javascript">
                                            document.getElementsByTagName("html")[0].className+=" lotusui_ie lotusui_ie6";
                                            </script>
                <![endif]-->
                <!--[if IE 7]>
                                            <script type="text/javascript">
                                            document.getElementsByTagName("html")[0].className+=" lotusui_ie lotusui_ie7";
                                            </script>
                <![endif]-->
            </head>
            <body class="lotusui lotusSpritesOn tundra">
                <div class="lotusFrame lotusWelcome">

                    <div class="lotusBanner" role="banner">
                        <div class="lotusRightCorner">
                            <div class="lotusInner">
                                <a href="#lotusMainContent" accesskey="S" class="lotusAccess">
                                    <img src="/oneuiv2.1/images/blank.gif"
                                        alt="Skip to main content link. Accesskey S"/>
                                </a>
                                <div class="lotusLogo">
                                    <span class="lotusAltText">Lotus &lt;Domino webDAV&gt;</span>
                                </div>

                                <ul class="lotusInlinelist lotusUtility">
                                    <li class="lotusFirst">
                                        <span class="lotusUser"><xsl:value-of select="//dxt:username[1]"/></span>
                                    </li>
                                </ul>

                            </div>
                        </div>
                    </div>
                    <!--end lotusBanner-->



                    <div class="lotusTitleBar">
                        <div class="lotusRightCorner">
                            <div class="lotusInner">

                                <ul class="lotusTabs" role="navigation">
                                    <li class="lotusSelected">
                                        <div>
                                            <a href="/webdav">Repositories</a>
                                        </div>
                                    </li>
                                    <li>
                                        <div>
                                            <a href="/webdavconfig">Configuration</a>
                                        </div>
                                    </li>
                                </ul>


                            </div>
                        </div>
                    </div>

                    <div class="lotusMain">

                        <div class="lotusColLeft">

                            <div class="lotusInfoBox lotusFirst" role="note">
                                <!--add the lotusFirst class to a tips box that sits at the top of a column-->

                                <h3>
                                    <span class="lotusLeft">About webDAV</span>
                                </h3>

                                <p><a href="http://www.webdav.org/">webDAV</a> is an open standard
                                    that allows you to access file read/write over HTTP. On Windows
                                    this service is known as webFolders. To make this work you want
                                    to install the helper application.</p>

                            </div>



                        </div>
                        <!--end colLeft-->
                        <a id="lotusMainContent" name="lotusMainContent"/>

                        <div class="lotusContent" role="main">
                            <div class="lotusWelcomeBox">

                                <h2>Domino webDAV Master Repository: <xsl:value-of select="/dav:multistatus/dav:response[1]/dav:href"/>
                                
                                </h2>

                                <p>The list of repositories is shown below. Additional repositories
                                    can be configured in the <a href="/webdavconfig">configuration
                                        tab</a>. Repositories give you access to files, attachments
                                    and other Domino assets. Usually you wouldn't visit this page
                                    but integrate Domino assets into your application directly.</p>
                                    <p>The installer runs completely silent in the blink of an eye. It only will prompt you
                                       when there is a problem. Not sure what it does? Download the source code too!</p>

                                <div class="lotusBtnContainer">
                                    <span class="lotusBtn lotusBtnSpecial">
                                        <a href="/webdav/DAVinternal/WebDocOpenSetup.exe">Install the
                                            helper application</a>
                                    </span>
									<span class="lotusBtn">
                                        <a href="/webdav/DAVinternal/WebDocOpen_src.zip">Download the source</a>
                                    </span>
                                </div>


                            </div>

                            <div class="lotusSection">

                                <div class="lotusHeader">

                                    <h1>Available repositories</h1>

                                    <table class="lotusTable lotusClear" border="0" cellspacing="0"
                                        cellpadding="0" summary="the list of repositories.">
                                        <tbody>
                                            <xsl:apply-templates select="dav:multistatus/dav:response" />

                                        </tbody>
                                    </table>

                                </div>
                            </div>

                        </div>
                    </div>

                    <table class="lotusLegal" cellspacing="0" role="presentation">
                        <tr>
                            <td>
                                <img class="lotusIBMLogoFooter" src="/oneuiv2.1/images/blank.gif"
                                    alt="IBM"/>
                            </td>
                            <td class="lotusLicense">(C) Copyright IBM Corporation 2010, 2011. All
                                Rights Reserved.</td>
                        </tr>
                    </table>



                </div>
            </body>
        </html>

    </xsl:template>

    <xsl:template match="dav:multistatus/dav:response">
        <tr>
        <td  class="lotusFirstCell" style="width:16px"><img src="/webdav/DAVinternal/folder.gif" alt="a repository" /></td>
            <td>
                <h5>
                    <xsl:element name="a">
                        <xsl:attribute name="href">
                            <xsl:value-of select="dav:href"/>
                        </xsl:attribute>
                        <xsl:value-of select="dav:propstat/dav:prop/dav:displayname"/>
                        <xsl:if test="dav:propstat/dav:prop/dav:displayname=''">
                            <xsl:value-of select="dav:href"/>
                        </xsl:if>
                    </xsl:element>
                </h5>
            </td>
        </tr>
    </xsl:template>

    <xsl:template match="dav:multistatus/dav:response[1]" />

</xsl:stylesheet>
