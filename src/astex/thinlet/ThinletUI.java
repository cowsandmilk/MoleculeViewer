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

    private HashSet<String> initialised = new HashSet<String>(11);

    /** Main entry point for scripting execution via thinlet. */
    private void execute(Object component){
        String init = (String)getProperty(component, "init");

        if(init != null && !initialised.contains(init)){
            initialised.add(init);

            init = preprocess(component, init);

            execute(init);
        }

        final String commands[] = { "precommand", "command", "postcommand" };

        for(int i = 0; i < commands.length; i++){
            String command = (String)getProperty(component, commands[i]);

            if(command == null && !"table".equals(getClass(component))){
                if("checkbox".equals(getClass(component))){
                    final boolean selected = getBoolean(component, "selected");
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
		final Object rows[] = getSelectedItems(component);

		final StringBuilder wholeCommand = new StringBuilder();

		for(int r = 0; r < rows.length; r++){
		    wholeCommand.append(preprocess(rows[r], command));
		}
		// do this per row...

		command = wholeCommand.toString();
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
        if(!command.endsWith(";")){
            command += ";";
        }

        moleculeRenderer.execute(command);

        moleculeViewer.dirtyRepaint();
    }

    /**
     * get the cell from the selected row that has
     * the specified columnName property.
     */
    private String getCellValueWithName(Object table, final String name){
        Object row = null;

        if("row".equals(getClass(table))){
            // it was a row really...
            row = table;
            table = getParent(row);
        }

        final Object header = getWidget(table, "header");

        int columnId = -1;

        for(int i = 0; i < getCount(header); i++){
            final Object column = getItem(header, i);
            final String text = getString(column, "text");
            if(text != null && text.equalsIgnoreCase(name)){
                columnId = i;
                break;
            }
        }

        if(columnId == -1){
            System.out.println("ERROR: table has no column called " + name);
        }

        if(row == null){
            // the original object was a table so we need to get the row
            // XXX will this ever actually get called this way?
            row = getSelectedItem(table);
        }

        final Object cell = getItem(row, columnId);

        return getString(cell, "text");
    }

    private String getValue(Object component, final String s){
        if("table".equals(getClass(component)) || "row".equals(getClass(component))){

            final String field = s.substring(1);

            final String value = getCellValueWithName(component, field);

            return value;
        }

        if("$o".equals(s)){
            return getBoolean(component, "selected") ? "on" : "off";
        }

        if("$b".equals(s)){
            return getBoolean(component, "selected") ? "true" : "false";
        }

        if("$t".equals(s)){
            return getString(component, "text");
        }

        if("$d".equals(s)){
            return Integer.toString(getInteger(component, "value"));
        }

        if("$V".equals(s)){
            return Double.toString(getInteger(component, "value") * 0.1);
        }

        if("$v".equals(s)){
            return Double.toString(getInteger(component, "value") * 0.01);
        }

        if("$c".equals(s)){
            final Color color = getColor(component, "background");
            return Color32.format(color.getRGB());
        }

        if("$C".equals(s)){
            final Color color = getColor(component, "background");
            return Color32.formatNoQuotes(color.getRGB());
        }

        if("$f".equals(s)){
            final Color color = getColor(component, "foreground");
            return Color32.formatNoQuotes(color.getRGB());
        }

        if("$n".equals(s)){
            if("combobox".equals(getClass(component))){
                component = getSelectedItem(component);
            }
            return getString(component, "name");
        }

        if("$h".equals(s)){
            double hsv[] = { 0.0, 1.0, 1.0 };
            hsv[0] = (double) getInteger(component, "value");
	    final int c = Color32.hsv2packed(hsv);
            return Color32.format(c);
        }

        final String property = (String)getProperty(component, s.substring(1));

        if(property != null){
            return property;
        }

        System.out.println("invalid attribute " + s);

        return null;
    }

    private String preprocess(final Object component, final String origCommand){
        return preprocess(component, origCommand, true);
    }

    private String preprocess(final Object component, final String origCommand, final boolean replacePipe){
        String command = origCommand;

        if(replacePipe){
            command = origCommand.replace('|', ';');
        }

        final StringBuilder newCommand = new StringBuilder(origCommand.length());

        try {
            // now do the values from other objects.
            final int len = command.length();
            for(int i = 0; i < len; i++){
                if(command.charAt(i) == '$'){
                    Object comp = component;
                    String attribute = null;
                    
                    if(command.charAt(++i) == '{'){
                        final StringBuilder sb = new StringBuilder(16);
                        while(command.charAt(++i) != '}'){
                            sb.append(command.charAt(i));
                        }

                        final String bits[] = FILE.split(sb.toString(), ".");
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


    public void windowClosing(final WindowEvent e){
	close(e.getWindow());
    }

    public void windowActivated(final WindowEvent e){ }
    public void windowClosed(final WindowEvent e){ }
    public void windowDeactivated(final WindowEvent e){ }
    public void windowDeiconified(final WindowEvent e){ }
    public void windowIconified(final WindowEvent e){ }
    public void windowOpened(final WindowEvent e){ }

    private void close(final Window window){
        window.setVisible(false);
        window.dispose();
    }

    public boolean destroy(){
        return false;
    }

    private void setContent(final String xml){
        try{
            add(parse(xml));
        }catch(Exception e){
            System.out.println("Exception: " + e);
        }
    }

    public ThinletUI(final MoleculeViewer mv, final String xml){
        this(mv);

        setContent(xml);
    }

    private ThinletUI(final MoleculeViewer mv){
        System.out.println("Thinlet GUI toolkit - www.thinlet.com");
        System.out.println("Copyright (C) 2002-2003 Robert Bajzat (robert.bajzat@thinlet.com)");

        moleculeViewer = mv;
        moleculeRenderer = mv.getMoleculeRenderer();
        
        moleculeRenderer.addMoleculeRendererListener(this);
        moleculeRenderer.renderer.addRendererEventListener(this);

	setFont(new Font("Arial", Font.PLAIN, 12));
    }

    public String readTemplate(final String xmlFile) {
        try {
            final FILE f = FILE.open(xmlFile);

            final InputStream fis = f.getInputStream();
            
            final StringBuilder sb = new StringBuilder(1024);
            
            int c = 0;
            
            while((c = fis.read()) != -1){
                sb.append((char)c);
            }
            
            fis.close();

            return sb.toString();
        }catch(Exception e){
            System.out.println("exception opening template " + e);
            return null;
        }
    }

    /** MoleculeRendererListener interface. */

    /** A molecule was added. */
    public void moleculeAdded(final MoleculeRenderer renderer, final Molecule molecule){
        final Object moleculeTree = findComponent("molecule_tree");

        addMolecule(moleculeTree, molecule);

        if(molecule.getMoleculeType() != Molecule.SymmetryMolecule){
            final Object resnode = findComponent("residuelist");
            final Object atomnode = findComponent("atomlist");
            
            if(resnode != null){
                populateResidues(resnode, atomnode);
            }
        }
    }

    private void addMolecule(final Object tree, final Molecule mol){
        if(tree != null){
            final Object node = createNode(mol.getName(), false, mol);

            final int chainCount = mol.getChainCount();

            for(int c = 0; c < chainCount; c++){
                final Chain chain = mol.getChain(c);

                String name = chain.getName();
                name = name.replace(' ', 'X');

                final Object chainNode = createNode(name, false, chain);
                final Object dummyNode = createNode("dummy", false, null);
                
                add(chainNode, dummyNode);
                add(node, chainNode);
            }
            
            add(tree, node);

            repaint();
        }else{
            System.out.println("couldn't find molecule tree");
        }
    }

    /** A molecule was removed. */
    public void moleculeRemoved(final MoleculeRenderer renderer, final Molecule molecule){

        removeMolecule(molecule);
    }

    private void removeMolecule(final Molecule molecule){
        final Object moleculeTree = find("molecule_tree");

        final Object molNode = find(moleculeTree, molecule.getName());

        remove(molNode);

        if(molecule.getMoleculeType() != Molecule.SymmetryMolecule){
            final Object resnode = findComponent("residuelist");
            final Object atomnode = findComponent("atomlist");
            
            if(resnode != null){
                populateResidues(resnode, atomnode);
            }
        }
    }

    private void populateResidues(final Object resnode, final Object atomnode){
        removeAll(resnode);
        removeAll(atomnode);
        final TreeSet<String> resnames = new TreeSet<String>();
        final TreeSet<String> atomnames = new TreeSet<String>();

        for(int m = 0; m < moleculeRenderer.getMoleculeCount(); m++){
            final Molecule mol = moleculeRenderer.getMolecule(m);
            for(int c = 0; c < mol.getChainCount(); c++){
                final Chain chain = mol.getChain(c);
                for(int r = 0; r < chain.getResidueCount(); r++){
                    final Residue res = chain.getResidue(r);
                    final String resname = res.getName();
                    if(!resnames.contains(resname)){
                        resnames.add(resname);
                    }
                    for(int a = 0; a < res.getAtomCount(); a++){
                        final Atom atom = res.getAtom(a);
                        final String atomname = atom.getAtomLabel();
                        if(!atomnames.contains(atomname)){
                            atomnames.add(atomname);
                        }
                    }
                }
            }
        }

        // residue names
        char lastChar = 0;
        Object folder = null;

        for(String name: resnames){
            final char c = name.charAt(0);
            if(c != lastChar){
                folder = create("node");
                setString(folder, "text", String.valueOf(c));
                setBoolean(folder, "expanded", false);

                add(resnode, folder);

                lastChar = c;
            }

            final Object node = create("node");
            setString(node, "text", name);
            putProperty(node, "selection", "name '" + name + "'");
            add(folder, node);
        }

        // atom names

        lastChar = 0;
        folder = null;

        for(String name: atomnames){
            final char c = name.charAt(0);
            if(c != lastChar){
                folder = create("node");
                setString(folder, "text", String.valueOf(c));
                setBoolean(folder, "expanded", false);

                add(atomnode, folder);

                lastChar = c;
            }

            final Object node = create("node");
            setString(node, "text", name);
            putProperty(node, "selection", "atom '" + name + "'");
            add(folder, node);
        }

    }

    public void genericAdded(final MoleculeRenderer renderer, final Generic generic){

        System.out.println("generic added " + generic);

        if(generic instanceof Distance){
            final Object list = findComponent("distance_list");

            addDistance(list, (Distance)generic);
        }
    }

    private void addDistance(final Object list, final Distance distance){
        
        if(list == null){
            return;
        }

        final String name = distance.getString(Generic.Name, "generic");

        final String itemString = "<item name=\"" + name + "\" text=\"" + name + "\"/>";

        final Object item = safeParse(itemString);

        putProperty(item, "reference", distance);

        add(list, item);
    }

    private String getColorString(final int rgb){
        return "#" + Integer.toHexString(rgb|0xff000000).substring(2);
    }

    private String getColorString2(final int rgb){
        return "0x" + Integer.toHexString(rgb|0xff000000).substring(2);
    }

    public void genericRemoved(final MoleculeRenderer renderer, final Generic generic){
        System.out.println("generic removed " + generic);

        final Object list = findComponent("distance_list");
        final Object item = find(list, (String)generic.get(Generic.Name, "generic"));

        remove(item);
    }

    /** A map was added. */
    public void mapAdded(final MoleculeRenderer renderer, final astex.Map map){
        System.out.println("mapAdded " + map);

        final Object mapComponent = findComponent("map_panel");

        addMap(mapComponent, map);
    }

    /** A map was removed. */
    public void mapRemoved(final MoleculeRenderer renderer, final astex.Map map){
        System.out.println("mapRemoved " + map);

        final Object mapComponent = findComponent("map_panel");

        final Object mapObject = find(mapComponent, map.getName());

        remove(mapObject);

        addMap(mapComponent, map);
    }

    private void addMap(final Object component, final astex.Map map){
        String mapTemplate = readTemplate("/astex/thinlet/maptemplate.xml.properties");

        for(int i = 0; i < astex.Map.MaximumContourLevels; i++){
            String contourTemplate = readTemplate("/astex/thinlet/contourtemplate.xml.properties");
            final int color = map.getContourColor(i);
            final String scolor = getColorString(color);

            contourTemplate = Util.replace(contourTemplate, "%contour", Integer.toString(i));
            contourTemplate = Util.replace(contourTemplate, "%color" + i, scolor);
            contourTemplate = Util.replace(contourTemplate, "%display" + i, Boolean.toString(map.getContourDisplayed(i)));

            String solid = "false";

            if(map.getContourStyle(i) == astex.Map.Surface){
                solid = "true";
            }
            contourTemplate = Util.replace(contourTemplate, "%solid" + i, solid);

            contourTemplate = Util.replace(contourTemplate, "%level" + i, Double.toString(map.getContourLevel(i)));

	    final Tmesh contourObject =
		moleculeRenderer.getContourGraphicalObject(map, i);

            contourTemplate = Util.replace(contourTemplate, "%width" + i, Double.toString(contourObject.getLineWidth()));

            mapTemplate = Util.replace(mapTemplate, "%c" + i, contourTemplate);
        }

        mapTemplate = Util.replace(mapTemplate, "%n", map.getName());
        // stupid, stupid, stupid, needs doing in loop above
        mapTemplate = Util.replace(mapTemplate, "%max", Double.toString(Math.max(map.getContourLevel(0), map.getContourLevel(1))));
        mapTemplate = Util.replace(mapTemplate, "%min", Double.toString(Math.min(map.getContourLevel(0), map.getContourLevel(1))));
        mapTemplate = Util.replace(mapTemplate, "%level2", Double.toString(map.getContourLevel(2)));
        mapTemplate = Util.replace(mapTemplate, "%color0", getColorString2(map.getContourColor(0)));
        mapTemplate = Util.replace(mapTemplate, "%color1", getColorString2(map.getContourColor(1)));
        mapTemplate = Util.replace(mapTemplate, "%color2", getColorString2(map.getContourColor(2)));

        add(component, safeParse(mapTemplate));
    }

    /** An atom was selected. */
    public void atomSelected(final MoleculeRenderer renderer, final Atom atom){
        if(atom == null){
            final Object moleculeTree = findComponent("molecule_tree");
            final Object items[] = getSelectedItems(moleculeTree);

            for(Object item: items){
                setBoolean(item, "selected", false);
            }

            repaint();
        }
    }

    public boolean handleRendererEvent(final RendererEvent re){
        if(re.getType() == RendererEvent.ObjectAdded){
            final Tmesh tmesh = (Tmesh)re.getItem();
            final Object objectList = findComponent("object_list");

            addObject(objectList, tmesh);
        }else if(re.getType() == RendererEvent.ObjectRemoved){
            final Tmesh tmesh = (Tmesh)re.getItem();

            remove(find(tmesh.getName()));
	}else if(re.getType() == RendererEvent.FrontClipMoved){
	    final Double d = (Double)re.getItem();
	    if(d != null){
		final double val = d.doubleValue();
                final Object clip = findComponent("frontclip");

                if(clip != null){
                    setString(clip, "text", String.format("%.1f", val));
                }
            }
	}else if(re.getType() == RendererEvent.BackClipMoved){
	    final Double d = (Double)re.getItem();
	    if(d != null){
		final double val = d.doubleValue();
                final Object clip = findComponent("backclip");
                if(clip != null){
                    setString(clip, "text", String.format("%.1f", val));
                }
            }
        }

        return true;
    }

    private void addObject(final Object component, final Tmesh object){
        if(component != null){
            String objectString =
                readTemplate("/astex/thinlet/objecttemplate.xml.properties");
            
            objectString = Util.replace(objectString, "%n", object.getName());
            objectString = Util.replace(objectString, "%c", Color32.formatNoQuotes(object.getColor()));
            
            add(component, safeParse(objectString));
        }
    }

    private Object createNode(final String name, final boolean expanded, final Object ref){
        final String nodeString =
            "<node text=\"" + name + "\" name=\"" + name + "\" expanded=\"" + expanded + "\" font=\"courier\"/>";

        final Object node = safeParse(nodeString);

        putProperty(node, "reference", ref);

        return node;
    }

    public Object safeParse(final String xml){
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
    private HashMap<String, Object> components = null;

    /**
     * Look up a cached object name.
     * Don't use this if the object may change.
     */
    private Object findComponent(final String name){
        if(components == null){
            components = new HashMap<String, Object>(11);
        }

        Object component = components.get(name);

        if(component == null){
            component = shallowFind(name);

            if(component == null){
                find(name);
            }

            if(component == null){
                System.out.println("WARNING: couldn't find thinlet component " + name);
            }else{
                components.put(name, component);
            }
        }

        return component;
    }
}
