# -*- coding: utf-8 -*-
"""
Draws patterns.svg: the State and Observer implementations, side by side.

    python patterns.py          -> patterns.svg
    chrome --headless=new --no-pdf-header-footer --print-to-pdf=patterns.pdf patterns.html
patterns.html only wraps the SVG at the right page size so Chrome prints
it as a single vector page.
Same box style and palette as gen.py, so the two diagrams read as a set.
"""
import io, os

W, H = 1680, 1188
HDR, LINE, PAD = 30, 16, 9
MONO = 'Consolas, Courier New, monospace'

PALETTE = {
    'abstract':  ('#eef2fb', '#3a5a9b'),
    'concrete':  ('#ffffff', '#5a6472'),
    'interface': ('#fdf3e3', '#b07d2a'),
    'core':      ('#e9f4fa', '#2d7899'),
    'view':      ('#f4eefb', '#7a4fa8'),
    'state':     ('#eef8ef', '#3f8a4d'),
}

out = []
A = out.append
boxes = {}


def esc(s):
    return s.replace('&', '&amp;').replace('<', '&lt;').replace('>', '&gt;')


def box(bid, x, y, w, name, kind, attrs, methods, stereo=None, note=None):
    n = len(attrs) + len(methods)
    extra = 18 if stereo else 0
    sep = 12 if (attrs and methods) else 0
    h = HDR + extra + PAD * 2 + n * LINE + sep + (LINE if note else 0)
    boxes[bid] = dict(x=x, y=y, w=w, h=h)
    fill, stroke = PALETTE[kind]
    A('<rect x="%d" y="%d" width="%d" height="%d" rx="5" fill="%s" stroke="%s" stroke-width="1.8"/>'
      % (x, y, w, h, fill, stroke))
    ty = y + 21
    if stereo:
        A('<text x="%.1f" y="%d" font-size="11.5" fill="%s" text-anchor="middle" font-style="italic">'
          '&#171;%s&#187;</text>' % (x + w / 2.0, ty, stroke, esc(stereo)))
        ty += 18
    A('<text x="%.1f" y="%d" font-size="16" font-weight="700" fill="#16202b" text-anchor="middle">%s</text>'
      % (x + w / 2.0, ty, esc(name)))
    ty += 11
    A('<line x1="%d" y1="%d" x2="%d" y2="%d" stroke="%s" stroke-width="1.2"/>' % (x, ty, x + w, ty, stroke))
    ty += 16
    for item in attrs:
        A('<text x="%d" y="%d" font-size="11.4" font-family="%s" fill="#33404f">%s</text>'
          % (x + 9, ty, MONO, esc(item)))
        ty += LINE
    if attrs and methods:
        ty -= 3
        A('<line x1="%d" y1="%d" x2="%d" y2="%d" stroke="%s" stroke-width="0.9" opacity="0.5"/>'
          % (x, ty, x + w, ty, stroke))
        ty += 17
    for item in methods:
        A('<text x="%d" y="%d" font-size="11.4" font-family="%s" fill="#33404f">%s</text>'
          % (x + 9, ty, MONO, esc(item)))
        ty += LINE
    if note:
        A('<text x="%d" y="%d" font-size="10.5" font-style="italic" fill="#7a8698">%s</text>'
          % (x + 9, ty, esc(note)))
    return boxes[bid]


def bottom(b): return (b['x'] + b['w'] / 2.0, b['y'] + b['h'])
def top(b):    return (b['x'] + b['w'] / 2.0, b['y'])


def path(d, kind):
    style = {
        'inherit': 'stroke="#3a4a5e" stroke-width="1.7" marker-end="url(#inh)"',
        'realize': 'stroke="#3a4a5e" stroke-width="1.7" stroke-dasharray="7,5" marker-end="url(#inh)"',
        'plain':   'stroke="#3a4a5e" stroke-width="1.7"',
        'plaindash': 'stroke="#3a4a5e" stroke-width="1.7" stroke-dasharray="7,5"',
        'compose': 'stroke="#2d7899" stroke-width="1.9" marker-start="url(#dia)" marker-end="url(#dep)"',
        'aggr':    'stroke="#2d7899" stroke-width="1.9" marker-start="url(#odia)" marker-end="url(#dep)"',
        'depend':  'stroke="#5a6472" stroke-width="1.4" stroke-dasharray="6,5" marker-end="url(#dep)"',
        'event':   'stroke="#b3403a" stroke-width="1.8" marker-end="url(#evt)"',
        'flow':    'stroke="#3a4a5e" stroke-width="1.6" marker-end="url(#dep)"',
    }[kind]
    A('<path d="%s" fill="none" %s/>' % (d, style))


def label(x, y, text, size=11, colour='#5a6472', anchor='start', italic=False, bold=False):
    A('<text x="%.1f" y="%.1f" font-size="%s" fill="%s" text-anchor="%s"%s%s>%s</text>'
      % (x, y, size, colour, anchor, ' font-style="italic"' if italic else '',
         ' font-weight="700"' if bold else '', esc(text)))


def bus(children, parent, bus_y, kind):
    """UML 'shared target' style: each child rises to one bar, one arrow goes up to the parent."""
    xs = [top(c)[0] for c in children]
    for c in children:
        cx, cy = top(c)
        path('M%.1f,%.1f V%.1f' % (cx, cy, bus_y), 'plaindash' if kind == 'realize' else 'plain')
    path('M%.1f,%.1f H%.1f' % (min(xs), bus_y, max(xs)), 'plaindash' if kind == 'realize' else 'plain')
    px, py = bottom(parent)
    path('M%.1f,%.1f V%.1f' % (px, bus_y, py), kind)


# ------------------------------------------------------------------ frame
A('<svg xmlns="http://www.w3.org/2000/svg" width="%d" height="%d" viewBox="0 0 %d %d" '
  'font-family="Segoe UI, Helvetica, Arial, sans-serif">' % (W, H, W, H))
A('<rect width="%d" height="%d" fill="#ffffff"/>' % (W, H))
A('<defs>')
A('<marker id="inh" markerWidth="14" markerHeight="14" refX="13" refY="7" orient="auto">'
  '<path d="M0,0 L13,7 L0,14 z" fill="#ffffff" stroke="#3a4a5e" stroke-width="1.4"/></marker>')
A('<marker id="dep" markerWidth="11" markerHeight="11" refX="10" refY="5.5" orient="auto">'
  '<path d="M0,0.5 L10,5.5 L0,10.5" fill="none" stroke="#5a6472" stroke-width="1.5"/></marker>')
A('<marker id="evt" markerWidth="11" markerHeight="11" refX="10" refY="5.5" orient="auto">'
  '<path d="M0,0.5 L10,5.5 L0,10.5 z" fill="#b3403a"/></marker>')
A('<marker id="dia" markerWidth="17" markerHeight="12" refX="1" refY="6" orient="auto">'
  '<path d="M0,6 L8,1.5 L16,6 L8,10.5 z" fill="#2d7899" stroke="#2d7899" stroke-width="1"/></marker>')
A('<marker id="odia" markerWidth="17" markerHeight="12" refX="1" refY="6" orient="auto">'
  '<path d="M0,6 L8,1.5 L16,6 L8,10.5 z" fill="#ffffff" stroke="#2d7899" stroke-width="1.3"/></marker>')
A('</defs>')

label(36, 44, 'Predator and Prey Simulation — Design Patterns', 25, '#1d2733', bold=True)
label(W - 36, 44, 'COMP2000 Final Submission · github.com/DanielZ195/COMP2000-Ecosystem',
      12.5, '#5a6472', anchor='end')
A('<line x1="850" y1="70" x2="850" y2="%d" stroke="#d5dbe3" stroke-width="1.5"/>' % (H - 50))

# ================================================================== STATE
label(36, 92, 'STATE — an animal’s behaviour each tick is decided by its current Mood object',
      15, '#3f8a4d', bold=True)

animal = box('animal', 36, 112, 380, 'Animal', 'abstract', [
    '# health : double',
], [
    '+ update(world)  // health -= METABOLISM',
    '      //   * getMood().metabolismRate()',
    '+ getMood() : Mood<?>     {abstract}',
], stereo='abstract')

mood = box('mood', 452, 112, 370, 'Mood<A extends Animal>', 'interface', [], [
    '+ act(a : A, w : World) : Mood<A>',
    '+ metabolismRate() : double  [= 1.0]',
    '+ name() : String',
    '+ marker() : Color           [= null]',
], stereo='interface  (State)')

prey = box('prey', 36, 296, 184, 'Prey', 'abstract', [
    '- mood : Mood<Prey>',
], [
    '# act(world)',
    '  mood = mood.act(',
    '         this, world)',
], stereo='context')

pred = box('pred', 232, 296, 184, 'Predator', 'abstract', [
    '- mood : Mood<Predator>',
], [
    '# act(world)',
    '  mood = mood.act(',
    '         this, world)',
], stereo='context')

# Prey / Predator extend Animal
bus([prey, pred], animal, 276, 'inherit')

# contexts hold one mood each -> Mood
px_, py_ = prey['x'] + prey['w'] / 2.0, prey['y'] + prey['h']
qx_, qy_ = pred['x'] + pred['w'] / 2.0, pred['y'] + pred['h']
path('M%.1f,%.1f V452 H436 V200 H452' % (px_, py_), 'compose')
path('M%.1f,%.1f V452' % (qx_, qy_), 'plain')
A('<text x="431" y="330" font-size="10.5" fill="#2d7899" font-style="italic" text-anchor="middle" '
  'transform="rotate(-90 431 330)">each holds exactly 1</text>')

moods_y = 492
gw, gap = 152, 6
xs = [36 + i * (gw + gap) for i in range(5)]
grazing = box('grazing', xs[0], moods_y, gw, 'Grazing', 'state', [
    'INSTANCE (shared)',
], [
    'rate 1.0',
    'eat / breed / roam',
])
fleeing = box('fleeing', xs[1], moods_y, gw, 'Fleeing', 'state', [
    '- lastThreatX, Y',
    '- calmIn = 3',
], [
    'rate 1.5  (panic)',
    'refuge / knight-hop',
])
resting = box('resting', xs[2], moods_y, gw, 'Resting', 'state', [
    'INSTANCE (shared)',
], [
    'rate 0.5',
    'sit still, breed',
])
hunting = box('hunting', xs[3], moods_y, gw, 'Hunting', 'state', [
    'INSTANCE (shared)',
], [
    'rate 1.0',
    'chase / mate / roam',
])
digesting = box('digesting', xs[4], moods_y, gw, 'Digesting', 'state', [
    '- ticksLeft = 4',
], [
    'rate 0.5',
    'stay put, breed',
])
bus([grazing, fleeing, resting, hunting, digesting], mood, 472, 'realize')
label(xs[0], moods_y + 128, 'implements Mood<Prey>', 10.5, '#3f8a4d', italic=True)
label(xs[3], moods_y + 128, 'implements Mood<Predator>', 10.5, '#3f8a4d', italic=True)
A('<line x1="%d" y1="%d" x2="%d" y2="%d" stroke="#3f8a4d" stroke-width="1" opacity="0.5"/>'
  % (xs[0], moods_y + 134, xs[2] + gw, moods_y + 134))
A('<line x1="%d" y1="%d" x2="%d" y2="%d" stroke="#3f8a4d" stroke-width="1" opacity="0.5"/>'
  % (xs[3], moods_y + 134, xs[4] + gw, moods_y + 134))

# ---------------- transition diagrams
label(36, 676, 'Transitions  (each arrow is a return statement inside act(); the switch and the new', 13,
      '#1d2733', bold=True)
label(36, 694, 'mood’s action happen in the same tick, so a prey never loses a step to panicking)', 13,
      '#1d2733', bold=True)


def node(x, y, w, text, colour='#3f8a4d', fill='#eef8ef', start=False):
    A('<rect x="%d" y="%d" width="%d" height="40" rx="20" fill="%s" stroke="%s" stroke-width="1.8"/>'
      % (x, y, w, fill, colour))
    label(x + w / 2.0, y + 26, text, 14, '#16202b', anchor='middle', bold=True)
    if start:
        A('<circle cx="%d" cy="%d" r="7" fill="#1d2733"/>' % (x - 34, y + 20))
        path('M%d,%d H%d' % (x - 27, y + 20, x - 2), 'flow')
    return dict(x=x, y=y, w=w)


label(36, 730, 'Prey', 13, '#5a6472', bold=True)
g = node(90, 800, 130, 'Grazing', start=True)
f = node(470, 740, 130, 'Fleeing')
r = node(470, 880, 130, 'Resting')

path('M220,806 C300,730 400,740 468,754', 'flow')
label(262, 738, 'predator in sight', 11.5, '#33404f')
path('M470,776 C380,812 300,836 222,830', 'flow')
label(262, 790, 'no predator for 3 ticks', 11.5, '#33404f')
path('M220,838 C300,866 400,880 468,892', 'flow')
label(300, 902, 'health ≥ 80%, no mate near', 11.5, '#33404f')
path('M470,914 C360,960 240,930 196,842', 'flow')
label(232, 966, 'health < 50%, or a mate in range', 11.5, '#33404f')
path('M560,878 V782', 'flow')
label(568, 836, 'predator', 11.5, '#33404f')
label(568, 850, 'in sight', 11.5, '#33404f')
label(640, 760, 'per-animal instance:', 10.5, '#7a8698', italic=True)
label(640, 774, 'remembers where the', 10.5, '#7a8698', italic=True)
label(640, 788, 'threat was last seen', 10.5, '#7a8698', italic=True)
label(640, 900, 'shared instance; the', 10.5, '#7a8698', italic=True)
label(640, 914, '80% / 50% gap stops', 10.5, '#7a8698', italic=True)
label(640, 928, 'flicker every tick', 10.5, '#7a8698', italic=True)

label(36, 990, 'Predator', 13, '#5a6472', bold=True)
hn = node(90, 1040, 130, 'Hunting', start=True)
dg = node(470, 1040, 140, 'Digesting')
path('M220,1052 C320,1020 380,1020 468,1052', 'flow')
label(296, 1018, 'tryEat() succeeds', 11.5, '#33404f')
path('M470,1072 C380,1104 320,1104 222,1072', 'flow')
label(286, 1118, 'after 4 ticks (ticksLeft = 0)', 11.5, '#33404f')
label(640, 1056, 'new instance per kill:', 10.5, '#7a8698', italic=True)
label(640, 1070, 'the countdown belongs', 10.5, '#7a8698', italic=True)
label(640, 1084, 'to one predator', 10.5, '#7a8698', italic=True)

# ================================================================== OBSERVER
OX = 878
label(OX, 92, 'OBSERVER — World announces what happened; anyone interested subscribes',
      15, '#b3403a', bold=True)

world = box('world', OX, 112, 300, 'World', 'core', [
    '- listeners : List<WorldListener>',
], [
    '+ addListener(l : WorldListener)',
    '+ removeListener(l : WorldListener)',
    '+ update()     → onTick, onEnded',
    '+ spawnNear()  → onBirth',
    '- removeDead() → onDeath',
    '~ recordEvade()→ onEvade',
], stereo='subject')

wl = box('wl', 1304, 112, 340, 'WorldListener', 'interface', [], [
    '+ onTick(w : World)                 {default}',
    '+ onBirth(child : Animal)           {default}',
    '+ onDeath(a, cause)               {default}',
    '+ onEvade(p : Prey)                 {default}',
    '+ onEnded(w, state)               {default}',
    '+ onTick(Consumer<World>)         {static}',
], stereo='interface  (Observer)')

wx = world['x'] + world['w']
path('M%d,%d H%d' % (wx, 180, wl['x']), 'aggr')
label(wx + 26, 172, '0..*', 10.5, '#2d7899', italic=True)
label(wx + 8, 200, 'notified in', 10.5, '#2d7899', italic=True)
label(wx + 8, 214, 'registration order', 10.5, '#2d7899', italic=True)

obs_y = 400
rec = box('rec', OX, obs_y, 330, 'PopulationRecorder', 'core', [
    '- history : Map<String, List<Integer>>',
    '- births, starved, eaten, hops : int',
    '- census : MoodCensus',
], [
    '+ onTick(w)  // groupingBy count +',
    '             //   collect(MoodCensus)',
    '+ onBirth / onDeath / onEvade  // ++',
    '+ totals() : String',
], stereo='observer')

con = box('con', 1222, obs_y, 196, 'ConsoleReporter', 'concrete', [
    '- recorder',
], [
    '+ onEnded(w, s)',
    '   // prints summary',
], stereo='observer')

lam = box('lam', 1430, obs_y, 214, 'Main’s listeners', 'view', [], [
    'onTick(w -> {',
    '  panel.repaint();',
    '  graph.repaint(); })',
    'new WorldListener() {',
    '  onEnded: timer.stop() }',
], stereo='lambda + anonymous')

bus([rec, con, lam], wl, 372, 'realize')

graph = box('graph', OX, 680, 330, 'GraphPanel', 'view', [], [
    '+ paintComponent(g)',
    '   // reads recorder.getHistory(),',
    '   //       totals(), getCensus()',
])
path('M%.1f,%d V%d' % (OX + 165, 680, rec['y'] + rec['h']), 'depend')
label(OX + 172, 670, 'reads', 10.5, '#5a6472', italic=True)

census = box('census', 1222, 680, 422, 'MoodCensus', 'core', [
    '- counts : Map<String, Integer>',
    '- meanHealth : Map<String, Double>',
], [
    '+ collector() : Collector<Animal, ?, MoodCensus>',
    '   supplier Tally::new   accumulator Tally::add',
    '   combiner Tally::combine  finisher Tally::finish',
], stereo='custom collector')
path('M%d,%d H%d V%d' % (OX + 330, 600, 1330, 680), 'depend')
label(1338, 650, 'builds one each tick', 10.5, '#5a6472', italic=True)
label(1222, 864, 'counts getMood().name() — where the two patterns meet', 11, '#3f8a4d', italic=True)

# ---------------- one tick, as a sequence strip
label(OX, 910, 'One tick, end to end', 13, '#1d2733', bold=True)
steps = [
    ('Swing Timer', 'world.update()'),
    ('each Animal', 'mood = mood.act()'),
    ('removeDead()', 'fires onDeath'),
    ('end of update', 'fires onTick'),
    ('Recorder', 'counts + census'),
    ('Main’s lambda', 'panels repaint'),
]
sx, sy, sw, sgap = OX, 930, 122, 6
for i, (who, what) in enumerate(steps):
    x = sx + i * (sw + sgap)
    colour = '#b3403a' if 'fires' in what else '#3a5a9b'
    A('<rect x="%d" y="%d" width="%d" height="58" rx="6" fill="#fafbfc" stroke="%s" stroke-width="1.5"/>'
      % (x, sy, sw, colour))
    label(x + sw / 2.0, sy + 22, who, 11.5, '#16202b', anchor='middle', bold=True)
    label(x + sw / 2.0, sy + 42, what, 10.5, colour, anchor='middle')
    if i < len(steps) - 1:
        path('M%d,%d H%d' % (x + sw, sy + 29, x + sw + sgap), 'event' if 'fires' in what else 'flow')

label(OX, 1022, 'Before: Main’s timer called update(), repainted both panels, checked for the end,',
      11.5, '#33404f')
label(OX, 1040, 'stopped itself and printed the totals, and World kept the statistics. After: the timer only',
      11.5, '#33404f')
label(OX, 1058, 'calls update(). Adding a new display or logger means one addListener() call, with no edit',
      11.5, '#33404f')
label(OX, 1076, 'to World or Main’s timer.', 11.5, '#33404f')

# ---------------- legend
ly = 1112
lx = OX
items = [('inherit', 'extends'), ('realize', 'implements'), ('compose', 'holds'),
         ('aggr', 'keeps a list of'), ('depend', 'uses'), ('event', 'fires event')]
for kind, text in items:
    if kind in ('compose', 'aggr'):
        path('M%d,%d H%d' % (lx + 4, ly, lx + 52), kind)
    else:
        path('M%d,%d H%d' % (lx, ly, lx + 46), kind)
    label(lx + 58, ly + 4, text, 11, '#33404f')
    lx += 58 + len(text) * 6.0 + 16

label(36, H - 22, 'Only the classes involved in the two patterns are drawn. The full class design is in design.pdf.',
      11, '#8a94a2')
A('</svg>')

here = os.path.dirname(os.path.abspath(__file__))
dest = os.path.join(here, 'patterns.svg')
io.open(dest, 'w', encoding='utf-8').write('\n'.join(out))
print('wrote %s' % dest)
