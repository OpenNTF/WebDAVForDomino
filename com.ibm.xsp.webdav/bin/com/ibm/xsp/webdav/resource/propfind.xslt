<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:dav="DAV:" xmlns:dxt="DXT:">
    <xsl:output method="html" indent="yes" doctype-public="-//W3C//DTD HTML 4.01 Transitional//EN"
        doctype-system="http://www.w3.org/TR/html4/loose.dtd"/>
    <xsl:template match="/">
        <html>
            <head>
                <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
                <title>Domino webDAV Repository  <xsl:value-of select="/dav:multistatus/dav:response[1]/dav:href[1]" /></title>
                <xsl:element name="meta">
                    <xsl:attribute name="http-equiv">Refresh</xsl:attribute>
                    <xsl:attribute name="content">180; url=<xsl:value-of select="/dav:multistatus/dav:response[1]/dav:href[1]" /></xsl:attribute>
                </xsl:element>
                <meta name="Description" content="Content of a repository exposed through webDAV"/>
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

                <script>
                  function davurl(orgurl) {
                    	var here = window.location.host;
                    	var prot = window.location.protocol;
                    	var newprot = prot.length == 5 ? "webdav:" : "webdavs:";
                    	var newURL = newprot+"//"+here+orgurl 
                    	window.location.assign(newURL);
                    }  
            
                  function upFolder(){
                    var pn=window.location.pathname; 
                    var arrPath=pn.split('/');
                    var s='';
                    var i=0;
                    for(i=0;i &lt; arrPath.length-1;i++){
                    	if(arrPath[i+1]!=''){
                    		s=s+arrPath[i]+'/';
                    		}
                    	}
                    window.location.assign(s);	
                    }
       
                </script></head>
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
                                    <li>
                                        <div>
                                            <a href="/webdav">Repositories</a>
                                        </div>
                                    </li>
                                    <li>
                                        <div>
                                            <a href="/webdavconfig">Configuration</a>
                                        </div>
                                    </li>
                                    <li class="lotusSelected">
                                        <div>
                                            <a href="#"><xsl:value-of select="/dav:multistatus/dav:response[1]/dav:href[1]" /></a>
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
                                    <span class="lotusLeft">webDAV Helper</span>
                                </h3>
                                
                                <p>To take full advantage of the webDAV round-trip editing capability you
                                    should install the helper application. It will make your machine aware
                                    of the webdav:// protocol and allows a seamless integration of your office applications</p>
                                <p>But even without it you can take advantage of webDAV using your webfolders in Windows.</p>
                                <p>Support on other platforms is native build in, including modern mobile devices</p>
                                
                                <div class="lotusBtnContainer">
                                    <span class="lotusBtn lotusBtnSpecial">
                                        <a href="/webdav/DAVinternal/WebDocOpenSetup.exe">Install the
                                            helper app</a>
                                    </span>
                                 </div>
                                 
                                    <p>The installer runs completely silent in the blink of an eye. It only will prompt you
                                       when there is a problem. Not sure what it does? Download the source code too!</p>
                                  <div class="lotusBtnContainer">      
                                    <span class="lotusBtn">
                                        <a href="/webdav/DAVinternal/WebDocOpen_src.zip">Download the source</a>
                                    </span>                                    
                                </div>
                            </div>
                        </div>
                        <!--end colLeft-->
                        <a id="lotusMainContent" name="lotusMainContent"/>
                        
                        <div class="lotusContent" role="main">
                            <div class="lotusWelcomeBox">
                                <h2>Domino webDAV Repository: <xsl:value-of select="/dav:multistatus/dav:response[1]/dav:href" /><span onclick="upFolder()"><img src="/webdav/DAVinternal/actn022.gif"/></span></h2>
                            </div>
                            <div class="lotusSection">
                                
                                <div class="lotusHeader">
                                    
                                    <h1>Available files and folders</h1>
                    				<span onclick="upFolder()"><h2>Up</h2></span>
                                     
                                    <table class="lotusTable lotusClear" border="0" cellspacing="0"
                                        cellpadding="0" summary="the list of files">
                                        <tbody>

                                            <xsl:apply-templates select="dav:multistatus/dav:response" >
                                             <xsl:sort select="dav:propstat/dav:prop/dav:displayname" />
                                            </xsl:apply-templates>
                                            
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
    
    <xsl:template match="dav:multistatus/dav:response[descendant::dav:collection]">
        <tr>
            <td  class="lotusFirstCell" style="width:16px"><img src="/webdav/DAVinternal/folder.gif" alt="a folder" /></td>
            <td>
                <xsl:element name="a">
                    <xsl:attribute name="style">font-weight: bold;</xsl:attribute>
                    <xsl:attribute name="href">
                        <xsl:value-of select="dav:href"/>
                    </xsl:attribute>
                    <xsl:value-of select="dav:propstat/dav:prop/dav:displayname"/>
                    <xsl:if test="dav:propstat/dav:prop/dav:displayname=''">
                        <xsl:value-of select="dav:href"/>
                    </xsl:if>
                </xsl:element>
            </td>
        </tr>
    </xsl:template>
    
    <xsl:template match="dav:multistatus/dav:response[not(descendant::dav:collection)]">
        <tr>
            <td class="lotusFirstCell" style="width:16px"><img src="/webdav/DAVinternal/files.gif" alt="a file" /></td>
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
                    <xsl:if test="dav:propstat/dav:prop/dav:lockdiscovery/dav:activelock/dav:owner">
                        <span class="lotusType lotusPrivate" style="color : red"> locked by 
                            <xsl:value-of select="dav:propstat/dav:prop/dav:lockdiscovery/dav:activelock/dav:owner" />
                        </span>
                    </xsl:if>
                </h5> 
                <div class="lotusMeta">
                    
                    <xsl:if test="dav:propstat/dav:prop/dav:getcontenttype">
                        <xsl:value-of select="dav:propstat/dav:prop/dav:getcontenttype" />
                        <span class="lotusDivider" role="separator">|</span>
                    </xsl:if>
                    
                    <xsl:if test="dav:propstat/dav:prop/dav:getcontentlength">
                        <xsl:value-of select="dav:propstat/dav:prop/dav:getcontentlength" />
                        <span class="lotusDivider" role="separator">|</span>
                    </xsl:if>
                    
                    <xsl:if test="dav:propstat/dav:prop/dav:creationdate">
                        created: <xsl:value-of select="dav:propstat/dav:prop/dav:creationdate" />
                        <span class="lotusDivider" role="separator">|</span>
                    </xsl:if>
                    
                    <xsl:if test="dav:propstat/dav:prop/dav:getlastmodified">
                        modified: <xsl:value-of select="dav:propstat/dav:prop/dav:getlastmodified" />
                        <span class="lotusDivider" role="separator">|</span>
                    </xsl:if>
                    <xsl:if test="not(dav:propstat/dav:prop/dav:lockdiscovery/dav:activelock/dav:owner)">
                    <xsl:element name="a">
                        <xsl:attribute name="onClick">JavaScript:davurl('<xsl:value-of select="dav:href"/>')</xsl:attribute>
                        Direct Open
                    </xsl:element>
                    </xsl:if>
                </div>                    
            </td>
        </tr>
    </xsl:template>
    
    <xsl:template match="dav:multistatus/dav:response[1]" />
    
</xsl:stylesheet>