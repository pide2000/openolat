(function() {
	tinymce.create('org.olat.core.gui.components.form.flexible.impl.elements.richText.plugins.olatmovieviewer', {

		/**
		 * Returns information about the plugin as a name/value array.
		 * The current keys are longname, author, authorurl, infourl and version.
		 *
		 * @returns Name/value array containing information about the plugin.
		 * @type Array 
		 */
		getInfo : function() {
			return {
				longname : 'OpenOLATMovieViewer',
				author : 'frentix GmbH',
				authorurl : 'http://www.frentix.com',
				infourl : 'http://www.frentix.com',
				version : '2.1.1'
			};
		},

		/**
		 * Not used, adButton used instead
		 */
		createControl : function(n, cm) {
			return null;
		},
	
		/**
		 * Initializes the plugin, this will be executed after the plugin has been created.
		 * This call is done before the editor instance has finished it's initialization so use the onInit event
		 * of the editor instance to intercept that event.
		 *
		 * @param {tinymce.Editor} ed Editor instance that the plugin is initialized in.
		 * @param {string} url Absolute URL to where the plugin is located.
		 */
		init : function(ed, url) {
			
			var cachedTrans;
			// Load the OLAT translator.
			function translator() {	
				if(cachedTrans) return cachedTrans;
				var mainWin = o_getMainWin();
				if (mainWin) {
					cachedTrans = jQuery(document).ooTranslator().getTranslator(mainWin.o_info.locale, 'org.olat.core.gui.components.form.flexible.impl.elements.richText.plugins.olatmovieviewer')	
				} else {
					cachedTrans = {	translate : function(key) { return key; } }
				}
				return cachedTrans;
			}
			
			function serializeParameters() {
				var d = document, f = d.forms[0], s = '';
				s += getStr(null, 'domIdentity');
				s += getStr(null, 'address');
				s += getStr(null, 'streamer');
				s += getStr(null, 'starttime');
				s += getBool(null, 'autostart');
				s += getBool(null, 'repeat');
				s += getBool(null, 'controlbar');
				s += getStr(null, 'provider');
				s += getStr(null, 'width');
				s += getStr(null, 'height');
				s = s.length > 0 ? s.substring(0, s.length - 1) : s;
				return s;
			}
			
			function deserializeParameters(pl, fe) {
				if (pl != "") {
					// Setup form from preselected item
					pl = eval(pl);
					setStr(pl, null, 'domIdentity');
					setStr(pl, null, 'address');
					setStr(pl, null, 'streamer');
					setStr(pl, null, 'starttime');
					setBool(pl, null, 'autostart');
					setBool(pl, null, 'repeat');
					setBool(pl, null, 'controlbar');
					setStr(pl, null, 'provider');
					setStr(pl, null, 'width');
					setStr(pl, null, 'height');
					
					if ((val = top.tinymce.activeEditor.dom.getAttrib(fe, "width")) != "") {
						pl.width = val;
					}
					
					if ((val = top.tinymce.activeEditor.dom.getAttrib(fe, "height")) != "") {
						pl.height = val;
					}
					
					oldWidth = pl.width ? parseInt(pl.width) : 0;
					oldHeight = pl.height ? parseInt(pl.height) : 0;
				} else {
					// Setup from with default values
					oldWidth = oldHeight = 0;
					var domIdentity = getNextDomId();
					var defaultPl = "x={domIdentity:'" + domIdentity + "',address:'',starttime:'00:00:00.000',autostart:false,repeat:false,controlbar:true};";
					deserializeParameters(defaultPl);
				}
			}
			
			function getNextDomId() {
				var count = 0;
				var domIdentity = "olatFlashMovieViewer";
				var placeHolders = top.tinymce.activeEditor.dom.select("img.mceItemOlatMovieViewer");
				do {
					domIdentity = "olatFlashMovieViewer" + (count++);
					if(count > 20) {
						break;
					}
				} while(domIdInUse(domIdentity, placeHolders));
				return domIdentity;
			}
			
			function domIdInUse(domIdentity,placeHolders) {
				for(var i=0; i<placeHolders.length; i++) {
					if(placeHolders[i].title != undefined && placeHolders[i].title.indexOf(domIdentity) > 0) {
						return true;
					}
				}
				return false;
			}
			
			function getBool(p, n, d, tv, fv) {
				var ctrl = win.find('#' + n)[0];
				var v = ctrl.checked();
				tv = typeof(tv) == 'undefined' ? 'true' : "'" + jsEncode(tv) + "'";
				fv = typeof(fv) == 'undefined' ? 'false' : "'" + jsEncode(fv) + "'";
				return (v == d) ? '' : n + (v ? ':' + tv + ',' : ':' + fv + ',');
			}

			function getStr(p, n, d) {
				var ctrl = win.find('#' + n)[0];
				var v = ctrl.value();
				return ((n == d || v == '') ? '' : n + ":'" + jsEncode(v) + "',");
			}

			function getInt(p, n, d) {
				var ctrl = win.find('#' + n)[0];
				var v = ctrl.value();
				return ((n == d || v == '') ? '' : n + ":" + v.replace(/[^0-9]+/g, '') + ",");
			}
			
			function setBool(pl, p, n) {
				if (typeof(pl[n]) == "undefined") return;
				var checked = (pl[n] == "true" ||  pl[n] == true ? true : false);
				win.find('#' + n)[0].checked(checked);
			}

			function setStr(pl, p, n) {
				if (typeof(pl[n]) == "undefined") return;
				win.find('#' + n)[0].value(pl[n]);
			}
			
			function jsEncode(s) {
				s = s.replace(new RegExp('\\\\', 'g'), '\\\\');
				s = s.replace(new RegExp('"', 'g'), '\\"');
				s = s.replace(new RegExp("'", 'g'), "\\'");
				return s;
			}
			
			function generatePreview() {
				var attribs = serializeParameters();
				var pl = eval("x={" + attribs + "}");
				_getEmbed(pl);

				if(pl.provider == "rtmp" || pl.provider == "http") {
					win.find('#streamer')[0].visible();
				} else {
					win.find('#streamer')[0].hide();
				}
				return;
			}
			
			function _getEmbed(p) {
				// player configuration
				var playerSrc = top.tinymce.activeEditor.getParam("olatmovieviewer_movieViewerUrl");
				var playerOffsetHeight = top.tinymce.activeEditor.getParam("olatmovieviewer_playerOffsetHeight");
				var playerOffsetWidth = top.tinymce.activeEditor.getParam("olatmovieviewer_playerOffsetWidth");
				var playerWidth = typeof(p.width) != "undefined" ? (parseInt(p.width) + parseInt(playerOffsetWidth))  : '320';
				var playerHeight = typeof(p.height) != "undefined" ? (parseInt(p.height) + parseInt(playerOffsetHeight))  : '240';
				var start = typeof(p.starttime) != "undefined" ? p.starttime : "00:00:00.000";
				var autostart = typeof(p.autostart) != "undefined" ? p.autostart : false;
				var repeat = typeof(p.repeat) != "undefined" ? p.repeat : false;
				var controlbar = typeof(p.controlbar) != "undefined" ? p.controlbar : true;
				var provider = typeof(p.provider) != "undefined" ? p.provider : undefined;
				var streamer = typeof(p.streamer) != "undefined" ? p.streamer : undefined;
				var domIdentity = typeof(p.domIdentity) != "undefined" ? p.domIdentity : getNextDomId();
				
				//scale the video if to big to not overlap the buttons
				var maxHeight = 400;
				var maxWidth = 560;
				if(playerHeight > maxHeight || playerWidth > maxWidth) {
					var thumbRatio = maxWidth / maxHeight;
				    var imageRatio = playerWidth / playerHeight;
				    if (thumbRatio < imageRatio) {
				    	playerHeight = (maxWidth / imageRatio);
				    	playerWidth = maxWidth;
				    }  else {
				    	playerWidth = (maxHeight * imageRatio);
				    	playerHeight = maxHeight;
				    }
				}
				
				var videoUrl = p.address
				if(p.address != undefined) {
					if(p.address.indexOf('://') < 0 && ((provider != "rtmp" && provider != "http") ||
						((provider == "rtmp" || provider == "http") && (streamer == undefined || streamer.length == 0)))) {
						videoUrl = top.tinymce.activeEditor.documentBaseURI.toAbsolute(p.address);
					}
				}
				
				if(p.address != undefined && p.address != null && p.address.length > 0) {
					BPlayer.insertPlayer(videoUrl,'prev_container',playerWidth,playerHeight,start,0,provider,streamer,autostart,repeat,controlbar);
				}
			}
			
			function buildProviderList() {
				var targetListItems = [
				    {text: translator().translate('olatmovieviewer.video'), value: 'video'},
				    {text: translator().translate('olatmovieviewer.sound'), value: 'sound'},
				    {text: translator().translate('olatmovieviewer.youtube'), value: 'youtube'},
				    {text: translator().translate('olatmovieviewer.http'), value: 'http'},
				    {text: translator().translate('olatmovieviewer.rtmp'), value: 'rtmp'}
				];
				return targetListItems;
			}
			
			function insertVideo() {
				var attribs = serializeParameters();
				var f = eval("x={" + attribs + "}");
				/*if (!AutoValidator.validate(f)) {
					alert(translator().translate("olatmovieviewer.invalid_date"));
					return false;
				}*/
				
				f.width = f.width == "" ? 100 : f.width;
				f.height = f.height == "" ? 100 : f.height;
				
				var fe = ed.selection.getNode();
				if (fe != null && fe != "undefined" && /mceItemOlatMovieViewer/.test(ed.dom.getAttrib(fe, 'class'))) {
					// change values from existing object
					if (fe.width != f.width.value || fe.height != f.width.height) {
						ed.execCommand("mceRepaint");
					}

					fe.title = serializeParameters();
					fe.width = f.width;
					fe.height = f.height;
					fe.style.width = f.width + (f.width.indexOf('%') == -1 ? 'px' : '');
					fe.style.height = f.height + (f.height.indexOf('%') == -1 ? 'px' : '');
				} else {
					// add new object
					var newDomId = getNextDomId();
					var titleAttr = "domIdentity:'" + newDomId + "'," + serializeParameters();
					var h = '<img id="' + newDomId + '"';
					h += ' class="mceItemOlatMovieViewer" src="' + ed.getParam("olatmovieviewer_transparentImage") + '"';
					h += ' title="' + titleAttr + '"';
					h += ' width="' + f.width + '"' + ' height="' + f.height + '" />';
					ed.execCommand("mceInsertContent", false, h);
				}
			} 
			
			function showDialog() {
				win = ed.windowManager.open({
					title: 'Movies',
					bodyType: 'tabpanel',
					body: [
					    {
					    	title: translator().translate('olatmovieviewer.general'),
					    	type: 'form',
					    	items: [
					    	    { name: 'provider', type: 'listbox', label: translator().translate('olatmovieviewer.provider'), values: buildProviderList() },
					    	    { name: 'streamer', type: 'textbox', label: translator().translate('olatmovieviewer.streamer')},
					    	    { name: 'address', type: 'filepicker', filetype: 'flashplayer', label: translator().translate('olatmovieviewer.address')},
					    	    {
									type: 'container',
									label: translator().translate('olatmovieviewer.size'),
									layout: 'flex',
									direction: 'row',
									align: 'center',
									spacing: 5,
									items: [
										{name: 'width', type: 'textbox', maxLength: 3, size: 3, onchange: generatePreview},
										{type: 'label', text: 'x'},
										{name: 'height', type: 'textbox', maxLength: 3, size: 3, onchange: generatePreview}
									]
								},
					    	    { name: 'preview', type: 'panel', label: '', minHeight: 320,
								  html:'<div id="prev" name="prev"><div id="prev_container" name="prev_container">&nbsp;</div></div>'
							    },
						    	{ name: 'domIdentity', type: 'textbox', hidden:true }
					    	]
					    },{
					    	title: 'Advanced',
					    	type: 'form',
					    	items: [
					    	    { name: 'starttime', type: 'textbox', checked: true, label: translator().translate('olatmovieviewer.starttime')},
					    	    { name: 'autostart', type: 'checkbox', checked: false, label: translator().translate('olatmovieviewer.autostart'), text:''},
					    	    { name: 'repeat', type: 'checkbox', checked: false, label: translator().translate('olatmovieviewer.repeat'), text:''},
					    	    { name: 'controlbar', type: 'checkbox', checked: false, label: translator().translate('olatmovieviewer.controlbar'), text:''}
					    	]
					    }],
					onSubmit: insertVideo
				});
				
				//fill the data
				var fe = ed.selection.getNode();
				if (/mceItemOlatMovieViewer/.test(ed.dom.getAttrib(fe, "class"))) {
					var pl = "x={" + ed.dom.getAttrib(fe, "title") + "};";
					deserializeParameters(pl, fe);
					setTimeout(generatePreview, 500);
				}		
			}
			
			function parseBPlayerScript(editor,script) {
				if(script == null || script == undefined) return '';
				var startMark = 'BPlayer.insertPlayer(';
				var start = script.indexOf(startMark);
				var end = script.indexOf(');');
				if(start < 0 || end < 0) return '';

				var playerOffsetHeight = editor.getParam("olatmovieviewer_playerOffsetHeight");
				var playerOffsetWidth = editor.getParam("olatmovieviewer_playerOffsetWidth");
				var params = script.substring(start + startMark.length,end);
				var settingsArr = params.split(',');
				var pl = 'domIdentity:' + settingsArr[1] + ',';
				pl += 'address:' + settingsArr[0] + ',';
				pl += 'streamer:' + settingsArr[7] + ',';
				pl += 'starttime:' + settingsArr[4] + ',';
				pl += 'autostart:' + settingsArr[8] + ',';
				pl += 'repeat:' + settingsArr[9] + ',';
				pl += 'controlbar:' + settingsArr[10] + ',';
				pl += 'provider:' + settingsArr[6] + ',';
				pl += 'width:' + (settingsArr[2] - playerOffsetWidth) + ',';
				pl += 'height:' + (settingsArr[3] - playerOffsetHeight);
				return pl;
			};
			
			//The video player code. Only one player per page supported.
			function getPlayerHtmlNode(editor,p) {
				var h = '', n, l = '';
				// player configuration
				var playerOffsetHeight = top.tinymce.activeEditor.getParam("olatmovieviewer_playerOffsetHeight");
				var playerOffsetWidth = top.tinymce.activeEditor.getParam("olatmovieviewer_playerOffsetWidth");
				var playerWidth = typeof(p.width) != "undefined" ? (parseInt(p.width) + parseInt(playerOffsetWidth))  : '';
				var playerHeight = typeof(p.height) != "undefined" ? (parseInt(p.height) + parseInt(playerOffsetHeight))  : '';
				var starttime = typeof(p.starttime) != "undefined" ? '"' + p.starttime + '"' : 0;
				var autostart = typeof(p.autostart) != "undefined" ? p.autostart : 'false';
				var repeat = typeof(p.repeat) != "undefined" ? p.repeat : 'false';
				var controlbar = typeof(p.controlbar) != "undefined" ? p.controlbar : 'true';
				var provider = typeof(p.provider) != "undefined" ? '"' + p.provider + '"' : 'undefined';
				var streamer = typeof(p.streamer) != "undefined" ? '"' + p.streamer + '"' : 'undefined';
				var domIdentity = typeof(p.domIdentity) != "undefined" ? p.domIdentity : getNextDomId();
				var playerScriptUrl = top.tinymce.activeEditor.getParam("olatmovieviewer_playerScript");

				var h = '<script src="' + playerScriptUrl + '" type="text/javascript"></script>';
				h += '<script type="text/javascript" defer="defer">';
				h += 'BPlayer.insertPlayer("' + p.address + '","' + domIdentity + '",' + playerWidth + ',' + playerHeight + ',' + starttime + ',0,' + provider + ',' + streamer +',' + autostart + ',' + repeat + ',' + controlbar + ');';
				h += '</script>';
				var node = ed.dom.create("span", {id:domIdentity,name:domIdentity,"class":"olatFlashMovieViewer",style:'display:block;border:solid 1px #000; width:' + playerWidth + 'px; height:' + playerHeight + 'px;'},h);
				return node;
			};

			ed.addButton('olatmovieviewer', {
				title : translator().translate('olatmovieviewer.desc'),
				image : url + '/images/movieviewer.gif',
				onclick: showDialog,
				onPostRender: function() {
			        var ctrl = this;
			        ed.on('NodeChange', function(e) {
						var test = (e.element.nodeName == 'IMG') && (/mceItemOlatMovieViewer/.test(ed.dom.getAttrib(e.element, 'class')));
						ctrl.active(test);
						if(test) {
							e.preventDefault(true);
							e.stopImmediatePropagation();
						}
					});
				}
			});
			
			ed.addMenuItem('olatmovieviewer', {
				text : translator().translate('olatmovieviewer.desc'),
				image : url + '/images/movieviewer.gif',
				onclick: showDialog,
			});

			// Load Content CSS upon initialization
			ed.on('init', function() {
			     if (ed.settings.content_css !== false) {
			    	 ed.dom.loadCSS(url + "/css/content.css");
			     }
			});
			
			/** 
             * This onPreProcess handler is used to convert the placeholder &lt;img&gt; tags to the
             * &lt;embed&gt; etc. tags when saving the document.
             */
			ed.on('PreProcess',function(editor) {
				// Find all IMGs of class "mceItemOlatMovieViewer"...
				tinymce.each(ed.dom.select("img.mceItemOlatMovieViewer"), function(node) {
					// ...read the movie settings out of the IMG's title attribute...
					var movieSettingsString = node.title;
					// ...clean up a bit...
					movieSettingsString = movieSettingsString.replace(/&(#39|apos);/g, "'");
					movieSettingsString = movieSettingsString.replace(/&#quot;/g, '"');
					var movieSettings;
					// ...parse the settings...
					try {
						movieSettings = eval("x={" + movieSettingsString + "}");
					} catch (exception) {
						movieSettings = {};
					}
					var playerNode = getPlayerHtmlNode(ed, movieSettings);
					ed.dom.replace(playerNode, node, false);
				});
			});
			
			//fallback for the old movies with settings in comments
			ed.on('BeforeSetContent',function(e) {
				if(e.content.indexOf('--omvs::') > 0) {
					var imgUrl = top.tinymce.activeEditor.getParam("olatmovieviewer_transparentImage");
					e.content = e.content.replace(/\n/gi, "");
					var widthMatch = e.content.match(/(?:<!--omvs::.*?width:')([0-9]+)(?:'.*?<!--omve-->)/i);
					var width = ((widthMatch != null) && (widthMatch.length == 2)) ? parseInt(widthMatch[1]) : 320;
					var heightMatch = e.content.match(/(?:<!--omvs::.*?height:')([0-9]+)(?:'.*?<!--omve-->)/i);
					var height = ((heightMatch != null) && (heightMatch.length == 2)) ? parseInt(heightMatch[1]) : 240;
					e.content = e.content.replace(/<!--omvs::(.*?)-->(.*?)<!--omve-->/gi, '<img class="mceItemOlatMovieViewer" alt="" src="' + imgUrl + '" title="$1" width="' + width + '" height="' + height + '"/>');
				}
			});
			
			/** 
             * This onSetContent handler is used to convert the comments to placeholder images (e.g. when loading).
             */
			ed.on('SetContent', function(e) {
				// Get the URL of the transparent placeholder image
				var imgUrl = ed.getParam("olatmovieviewer_transparentImage");
				tinymce.each(ed.dom.select("div.olatFlashMovieViewer,span.olatFlashMovieViewer"), function(node) {
					// ...and for each of these, create an IMG...
					var movieSettingsString = parseBPlayerScript(ed,node.innerHTML);
					var movieSettings;
					try {
						movieSettings = eval("x={" + movieSettingsString + "}");
					} catch (exception) {
						movieSettings = {};
					}
					var imgNode = ed.dom.create("img", {id:movieSettings.domIdentity,name:movieSettings.domIdentity,"class":"mceItemOlatMovieViewer", src:imgUrl, title:movieSettingsString});
					//for ie8
					imgNode.width = typeof(movieSettings.width) == 'undefined' ? 320 : movieSettings.width;
					imgNode.height = typeof(movieSettings.height) == 'undefined' ? 240 : movieSettings.height;
					//  ...and replace the div by the new img.
					ed.dom.replace(imgNode, node, false);
			    });
			});
		}
	});

	// Register plugin
	tinymce.PluginManager.add('olatmovieviewer', org.olat.core.gui.components.form.flexible.impl.elements.richText.plugins.olatmovieviewer);
})();