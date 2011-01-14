/*
 * This file is part of OpenAstexViewer.
 *
 * OpenAstexViewer is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * OpenAstexViewer is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with OpenAstexViewer.  If not, see <http://www.gnu.org/licenses/>.
 */

package astex.thinlet;

import astex.*;
import astex.splitter.*;
import astex.generic.*;
import thinlet.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

public class ThinletUI extends Thinlet implements WindowListener,
                                                  MoleculeRendererListener,
                                                  RendererEventListener {
    public MoleculeViewer moleculeViewer     = null;
    public MoleculeRenderer moleculeRenderer = null;

    private Hashtable<String, String> initialised = new Hashtable<String,String>(11);

    /** Main entry point for scripting execution via thinlet. */
    private void execute(Object component){
        String init = (String)getProperty(component, "init");

        if(init != null && initialised.get(init) == null){
            initialised.put(init, init);

            init = preprocess(component, init);

            execute(init);
        }

        String commands[] = { "precommand", "command", "postcommand" };

        for(int i = 0; i < commands.length; i++){
            String command = (String)getProperty(component, commands[i]);

            if(command == null && !"table".equals(getClass(component))){
                if("checkbox".equals(getClass(component))){
                    boolean selected = getBoolean(component, "selected");
                    if(selected){
                        command = (String)getProperty(component, "commandon");
                    }else{
                        command = (String)getProperty(component, "commandoff");
                    }
                }else if("tabbedpane".equals(getClass(component))){
                    component = getSelectedItem(component);
                    
                    if(getProperty(component, "command") != null){
                        execute(component);
                    }
                    
                    return;
                    // this next block was commented out - why?
                    // must have caused some other effect, restrict it to a textarea
                }else if("textarea".equals(getClass(component))){
                    command = getString(component, "text");
                }
            }else if(command != null && ("table".equals(getClass(component)) || "tree".equals(getClass(component))) && i == 1){
		// only preprocess the pertable command
		Object rows[] = getSelectedItems(component);

		String wholeCommand = "";

		for(int r = 0; r < rows.length; r++){
		    wholeCommand += preprocess(rows[r], command);
		}
		// do this per row...

		command = wholeCommand;
            }

            if(command != null){
                command = preprocess(component, command);

                execute(command);
            }else{
                if(i == 1){
                    Log.error("no command or text to execute");
                }
            }
        }
    }

    /** Actually execute a command and repaint. */
    private void execute(String command){
        if(command.endsWith(";") == false){
            command += ";";
        }

        moleculeRenderer.execute(command);

        moleculeViewer.dirtyRepaint();
    }

    /**
     * get the cell from the selected row that has
     * the specified columnName property.
     */
    private String getCellValueWithName(Object table, String name){
        Object row = null;

        if(getClass(table).equals("row")){
            // it was a row really...
            row = table;
            table = getParent(row);
        }

        Object header = getWidget(table, "header");

        int columnId = -1;

        for(int i = 0; i < getCount(header); i++){
            Object column = getItem(header, i);
            String text = getString(column, "text");
            if(text != null && text.equalsIgnoreCase(name)){
                columnId = i;
                break;
            }
        }

        if(columnId == -1){
            print.f("ERROR: table has no column called " + name);
        }

        if(row == null){
            // the original object was a table so we need to get the row
            // XXX will this ever actually get called this way?
            row = getSelectedItem(table);
        }

        Object cell = getItem(row, columnId);

        return getString(cell, "text");
    }

    private String getValue(Object component, String s){
        if("table".equals(getClass(component)) || "row".equals(getClass(component))){

            String field = s.substring(1);

            String value = getCellValueWithName(component, field);

            return value;
        }

        if("$o".equals(s)){
            boolean selected = getBoolean(component, "selected");
            return selected ? "on" : "off";
        }

        if("$b".equals(s)){
            boolean selected = getBoolean(component, "selected");
            return selected ? "true" : "false";
        }

        if("$t".equals(s)){
            return getString(component, "text");
        }

        if("$d".equals(s)){
            return "" + getInteger(component, "value");
        }

        if("$V".equals(s)){
            return "" + getInteger(component, "value") * 0.1;
        }

        if("$v".equals(s)){
            return "" + getInteger(component, "value") * 0.01;
        }

        if("$c".equals(s)){
            Color color = getColor(component, "background");
            return Color32.format(color.getRGB());
        }

        if("$C".equals(s)){
            Color color = getColor(component, "background");
            return Color32.formatNoQuotes(color.getRGB());
        }

        if("$f".equals(s)){
            Color color = getColor(component, "foreground");
            return Color32.formatNoQuotes(color.getRGB());
        }

        if("$n".equals(s)){
            if("combobox".equals(getClass(component))){
                component = getSelectedItem(component);
            }
            String name = getString(component, "name");
            return name;
        }

        if("$h".equals(s)){
            double hsv[] = { 0.0, 1.0, 1.0 };
            int value = getInteger(component, "value");
            hsv[0] = (double)value;
	    int c = Color32.hsv2packed(hsv);
            return Color32.format(c);
        }

        String property = (String)getProperty(component, s.substring(1));

        if(property != null){
            return property;
        }

        System.out.println("invalid attribute " + s);

        return null;
    }

    private String preprocess(Object component, String origCommand){
        return preprocess(component, origCommand, true);
    }

    private String preprocess(Object component, String origCommand, boolean replacePipe){
        String command = origCommand;

        if(replacePipe){
            command = origCommand.replace('|', ';');
        }

        StringBuilder newCommand = new StringBuilder(origCommand.length());

        try {
            // now do the values from other objects.
            int len = command.length();
            for(int i = 0; i < len; i++){
                if(command.charAt(i) == '$'){
                    Object comp = component;
                    String attribute = null;
                    
                    if(command.charAt(++i) == '{'){
                        StringBuilder sb = new StringBuilder(16);
                        while(command.charAt(++i) != '}'){
                            sb.append(command.charAt(i));
                        }

                        String bits[] = FILE.split(sb.toString(), ".");
                        if(bits.length != 2){
                            System.out.println("no . character");
                            System.out.println(origCommand);
                        }else{
                            // this should cache the object name...
                            comp = findComponent(bits[0]);
                            attribute = "$" + bits[1];
                            if(comp == null){
                                System.out.println("couldn't find object " + bits[0]);
                            }
                        }
                    }else{
                        attribute = "$" + command.charAt(i);
                    }

                    String value = null;

                    if(comp == getParent(component)){
                        // if the named table was our parent then
                        // we just go for the row instead
                        value = getValue(component, attribute);
                    }else if("tree".equals(getClass(comp))){
                        value = getValue(getSelectedItem(comp), attribute);
                    }else{
                        value = getValue(comp, attribute);
                    }

                    if(value == null){
                        // just append what was there...
                        newCommand.append(attribute);
                    }else{
                        newCommand.append(value);
                    }
                }else{
                    newCommand.append(command.charAt(i));
                }
            }
        }catch(Exception e){
            System.out.println("error processing command: " + origCommand);
            System.out.println("exception " + e);
            return null;
        }

        return newCommand.toString();
    }


    public void windowClosing(WindowEvent e){
	close(e.getWindow());
    }

    public void windowActivated(WindowEvent e){ }
    public void windowClosed(WindowEvent e){ }
    public void windowDeactivated(WindowEvent e){ }
    public void windowDeiconified(WindowEvent e){ }
    public void windowIconified(WindowEvent e){ }
    public void windowOpened(WindowEvent e){ }

    private void close(Window window){
        window.setVisible(false);
        window.dispose();
    }

    public boolean destroy(){
        return false;
    }

    private void setContent(String xml){
        try{
            add(parse(xml));
        }catch(Exception e){
            System.out.println("Exception: " + e);
        }
    }

    public ThinletUI(MoleculeViewer mv, String xml){
        this(mv);

        setContent(xml);
    }

    private ThinletUI(MoleculeViewer mv){
        System.out.println("Thinlet GUI toolkit - www.thinlet.com");
        System.out.println("Copyright (C) 2002-2003 Robert Bajzat (robert.bajzat@thinlet.com)");

        moleculeViewer = mv;
        moleculeRenderer = mv.getMoleculeRenderer();
        
        moleculeRenderer.addMoleculeRendererListener(this);
        moleculeRenderer.renderer.addRendererEventListener(this);

	setFont(new Font("Arial", Font.PLAIN, 12));
    }

    public String readTemplate(String xmlFile) {
        try {
            FILE f = FILE.open(xmlFile);

            InputStream fis = f.getInputStream();
            
            StringBuffer sb = new StringBuffer(1024);
            
            int c = 0;
            
            while((c = fis.read()) != -1){
                sb.append((char)c);
            }
            
            fis.close();

            return sb.toString();
        }catch(Exception e){
            print.f("exception opening template " + e);
            return null;
        }
    }

    /** MoleculeRendererListener interface. */

    /** A molecule was added. */
    public void moleculeAdded(MoleculeRenderer renderer, Molecule molecule){
        Object moleculeTree = findComponent("molecule_tree");

        addMolecule(moleculeTree, molecule);

        if(molecule.getMoleculeType() != Molecule.SymmetryMolecule){
            Object resnode = findComponent("residuelist");
            Object atomnode = findComponent("atomlist");
            
            if(resnode != null){
                populateResidues(resnode, atomnode);
            }
        }
    }

    private void addMolecule(Object tree, Molecule mol){
        if(tree != null){
            Object node = createNode(mol.getName(), false, mol);

            int chainCount = mol.getChainCount();

            for(int c = 0; c < chainCount; c++){
                Chain chain = mol.getChain(c);

                String name = chain.getName();
                name.replace(' ', 'X');

                Object chainNode = createNode(name, false, chain);
                Object dummyNode = createNode("dummy", false, null);
                
                add(chainNode, dummyNode);
                add(node, chainNode);
            }
            
            add(tree, node);

            repaint();
        }else{
            print.f("couldn't find molecule tree");
        }
    }

    /** A molecule was removed. */
    public void moleculeRemoved(MoleculeRenderer renderer, Molecule molecule){

        removeMolecule(molecule);
    }

    private void removeMolecule(Molecule molecule){
        Object moleculeTree = find("molecule_tree");

        Object molNode = find(moleculeTree, molecule.getName());

        remove(molNode);

        if(molecule.getMoleculeType() != Molecule.SymmetryMolecule){
            Object resnode = findComponent("residuelist");
            Object atomnode = findComponent("atomlist");
            
            if(resnode != null){
                populateResidues(resnode, atomnode);
            }
        }
    }

    private void populateResidues(Object resnode, Object atomnode){
        removeAll(resnode);
        removeAll(atomnode);
        Hashtable<String,String> resnames = new Hashtable<String,String>(100);
        Hashtable<String,String> atomnames = new Hashtable<String, String>(100);

        for(int m = 0; m < moleculeRenderer.getMoleculeCount(); m++){
            Molecule mol = moleculeRenderer.getMolecule(m);
            for(int c = 0; c < mol.getChainCount(); c++){
                Chain chain = mol.getChain(c);
                for(int r = 0; r < chain.getResidueCount(); r++){
                    Residue res = chain.getResidue(r);
                    String resname = res.getName();
                    if(resnames.contains(resname) == false){
                        resnames.put(resname, resname);
                    }
                    for(int a = 0; a < res.getAtomCount(); a++){
                        Atom atom = res.getAtom(a);
                        String atomname = atom.getAtomLabel();
                        if(atomnames.contains(atomname) == false){
                            atomnames.put(atomname, atomname);
                        }
                    }
                }
            }
        }

        // residue names
        String names[] = new String[resnames.size()];
        int count = 0;

        Enumeration<String> k = resnames.elements();

        while(k.hasMoreElements()){
            String name = k.nextElement();
            names[count++] = name;
        }

        sort(names, count);

        char lastChar = 0;
        Object folder = null;

        for(int i = 0; i < count; i++){
            char c = names[i].charAt(0);
            if(c != lastChar){
                folder = create("node");
                setString(folder, "text", "" + c);
                setBoolean(folder, "expanded", false);

                add(resnode, folder);

                lastChar = c;
            }

            Object node = create("node");
            setString(node, "text", names[i]);
            putProperty(node, "selection", "name '" + names[i] + "'");
            add(folder, node);
        }

        // atom names

        count = 0;
        names = new String[atomnames.size()];
        k = atomnames.elements();

        while(k.hasMoreElements()){
            String name = k.nextElement();
            names[count++] = name;
        }

        sort(names, count);

        lastChar = 0;
        folder = null;

        for(int i = 0; i < count; i++){
            char c = names[i].charAt(0);
            if(c != lastChar){
                folder = create("node");
                setString(folder, "text", "" + c);
                setBoolean(folder, "expanded", false);

                add(atomnode, folder);

                lastChar = c;
            }

            Object node = create("node");
            setString(node, "text", names[i]);
            putProperty(node, "selection", "atom '" + names[i] + "'");
            add(folder, node);
        }

    }

    private void sort(String a[], int n){
	for (int i = n; --i >= 0; ) {
            boolean flipped = false;
	    for (int j = 0; j < i; j++) {
		if (a[j].compareTo(a[j+1]) > 0){
		    String T = a[j];
		    a[j] = a[j+1];
		    a[j+1] = T;
		    flipped = true;
		}
	    }
	    if (!flipped) {
	        return;
	    }
        }
    }

    public void genericAdded(MoleculeRenderer renderer, Generic generic){

        System.out.println("generic added " + generic);

        if(generic instanceof Distance){
            Object list = findComponent("distance_list");

            addDistance(list, (Distance)generic);
        }
    }

    private void addDistance(Object list, Distance distance){
        
        if(list == null){
            return;
        }

        String name = distance.getString(Generic.Name, "generic");

        String itemString = "<item name=\"" + name + "\" text=\"" + name + "\"/>";

        Object item = safeParse(itemString);

        putProperty(item, "reference", distance);

        add(list, item);
    }

    private String getColorString(int rgb){
        return "#" + Integer.toHexString(rgb|0xff000000).substring(2);
    }

    private String getColorString2(int rgb){
        return "0x" + Integer.toHexString(rgb|0xff000000).substring(2);
    }

    public void genericRemoved(MoleculeRenderer renderer, Generic generic){
        System.out.println("generic removed " + generic);

        Object list = findComponent("distance_list");
        Object item = find(list, (String)generic.get(Generic.Name, "generic"));

        remove(item);
    }

    /** A map was added. */
    public void mapAdded(MoleculeRenderer renderer, astex.Map map){
        System.out.println("mapAdded " + map);

        Object mapComponent = findComponent("map_panel");

        addMap(mapComponent, map);
    }

    /** A map was removed. */
    public void mapRemoved(MoleculeRenderer renderer, astex.Map map){
        System.out.println("mapRemoved " + map);

        Object mapComponent = findComponent("map_panel");

        Object mapObject = find(mapComponent, map.getName());

        remove(mapObject);

        addMap(mapComponent, map);
    }

    private void addMap(Object component, astex.Map map){
        String mapTemplate = readTemplate("/astex/thinlet/maptemplate.xml.properties");

        for(int i = 0; i < astex.Map.MaximumContourLevels; i++){
            String contourTemplate = readTemplate("/astex/thinlet/contourtemplate.xml.properties");
            int color = map.getContourColor(i);
            String scolor = getColorString(color);

            contourTemplate = Util.replace(contourTemplate, "%contour", "" + i);
            contourTemplate = Util.replace(contourTemplate, "%color" + i, scolor);
            contourTemplate = Util.replace(contourTemplate, "%display" + i, "" + map.getContourDisplayed(i));

            String solid = "false";

            if(map.getContourStyle(i) == astex.Map.Surface){
                solid = "true";
            }
            contourTemplate = Util.replace(contourTemplate, "%solid" + i, solid);

            contourTemplate = Util.replace(contourTemplate, "%level" + i, "" + map.getContourLevel(i));

	    Tmesh contourObject =
		moleculeRenderer.getContourGraphicalObject(map, i);

            contourTemplate = Util.replace(contourTemplate, "%width" + i, "" + contourObject.getLineWidth());

            mapTemplate = Util.replace(mapTemplate, "%c" + i, contourTemplate);
        }

        mapTemplate = Util.replace(mapTemplate, "%n", map.getName());
        // stupid, stupid, stupid, needs doing in loop above
        mapTemplate = Util.replace(mapTemplate, "%max", "" + Math.max(map.getContourLevel(0), map.getContourLevel(1)));
        mapTemplate = Util.replace(mapTemplate, "%min", "" + Math.min(map.getContourLevel(0), map.getContourLevel(1)));
        mapTemplate = Util.replace(mapTemplate, "%level2", "" + map.getContourLevel(2));
        mapTemplate = Util.replace(mapTemplate, "%color0", getColorString2(map.getContourColor(0)));
        mapTemplate = Util.replace(mapTemplate, "%color1", getColorString2(map.getContourColor(1)));
        mapTemplate = Util.replace(mapTemplate, "%color2", getColorString2(map.getContourColor(2)));

        add(component, safeParse(mapTemplate));
    }

    /** An atom was selected. */
    public void atomSelected(MoleculeRenderer renderer, Atom atom){
        //System.out.println("atomSelected " + atom);

        if(atom == null){
            Object moleculeTree = findComponent("molecule_tree");
            Object items[] = getSelectedItems(moleculeTree);

            for(int i = 0; i < items.length; i++){
                setBoolean(items[i], "selected", false);
            }

            repaint();
        }
    }

    public boolean handleRendererEvent(RendererEvent re){
        if(re.getType() == RendererEvent.ObjectAdded){
            Tmesh tmesh = (Tmesh)re.getItem();
            Object objectList = findComponent("object_list");

            addObject(objectList, tmesh);
        }else if(re.getType() == RendererEvent.ObjectRemoved){
            Tmesh tmesh = (Tmesh)re.getItem();

            remove(find(tmesh.getName()));
	}else if(re.getType() == RendererEvent.FrontClipMoved){
	    Double d = (Double)re.getItem();
	    if(d != null){
		double val = d.doubleValue();
                Object clip = findComponent("frontclip");

                if(clip != null){
                    setString(clip, "text", FILE.sprint("%.1f", val));
                }
            }
	}else if(re.getType() == RendererEvent.BackClipMoved){
	    Double d = (Double)re.getItem();
	    if(d != null){
		double val = d.doubleValue();
                Object clip = findComponent("backclip");
                if(clip != null){
                    setString(clip, "text", FILE.sprint("%.1f", val));
                }
            }
        }

        return true;
    }

    private void addObject(Object component, Tmesh object){
        if(component != null){
            String objectString =
                readTemplate("/astex/thinlet/objecttemplate.xml.properties");
            
            objectString = Util.replace(objectString, "%n", object.getName());
            objectString = Util.replace(objectString, "%c", Color32.formatNoQuotes(object.getColor()));
            
            add(component, safeParse(objectString));
        }
    }

    private Object createNode(String name, boolean expanded, Object ref){
        String nodeString =
            "<node text=\"" + name + "\" name=\"" + name + "\" expanded=\"" + expanded + "\" font=\"courier\"/>";

        Object node = safeParse(nodeString);

        putProperty(node, "reference", ref);

        return node;
    }

    public Object safeParse(String xml){
        try {
            return parse(new StringReader(xml));
        }catch(Exception e){
            System.out.println("error parsing xml: " + xml);
            e.printStackTrace();

            return null;
        }
    }

    public ThinletUI(){
    }

    /** Hashtable of component names to objects. */
    private Hashtable<String, Object> components = null;

    /**
     * Look up a cached object name.
     * Don't use this if the object may change.
     */
    private Object findComponent(String name){
        if(components == null){
            components = new Hashtable<String, Object>(11);
        }

        Object component = components.get(name);

        if(component == null){
            component = shallowFind(name);

            if(component == null){
                find(name);
            }

            if(component == null){
                print.f("WARNING: couldn't find thinlet component " + name);
            }else{
                components.put(name, component);
            }
        }

        return component;
    }
}
