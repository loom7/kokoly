# SPDX-License-Identifier: GPL-3.0-or-later
"""
Der Golden-Korpus: die Sätze, an denen die Android-Kette ihre Gleichheit mit
der Windows-Referenz beweisen muss.

Die deutschen Sätze sind nicht zufällig — jeder deckt einen belegten Fehlerfall
oder eine Frontend-Falle ab (Verweise: docs/erkenntnisse.md). Die anderen
Sprachen bekommen je eine kleine, lautlich breite Auswahl samt ihrer
Interpunktions-Eigenheiten (¿…?, Apostrophe, Devanagari-Danda).
"""

KORPUS = {
    "de": [
        # DER Gate-Satz: dichtester ich-Laut-Gehalt — der historische Fehlschlag.
        "Ich möchte nicht, dass mich vielleicht doch jemand richtig versteht.",
        # Minimalpaar ç/x im selben Satz.
        "Das Frauchen ruft, wir rauchen nicht.",
        # Kurzes ü (ʏ→y-Lautersatz) in fünf Wörtern.
        "Fünf Mütter gingen über die Brücke in München, jede mit Glück.",
        # Sprachwechselmarken: (en)…(de) muss verschwinden, ohne zu sprechen.
        "Das Team schickt ein Update per E-Mail an den Server.",
        # Echte Klammern müssen die Markenentfernung überleben.
        "Ein Text (in Klammern) hier.",
        # Klammern, die wie Marken aussehen.
        "Der Artikel (das) und der Artikel (der).",
        # Alltagssätze der bisherigen Prüfungen.
        "Guten Tag, hier spricht Martin.",
        "Der Termin ist am dritten August um halb zehn.",
        "Die Erbse ist grün und die Erbsen sind gekocht.",
        "Ich lese das Journal in der Etage.",
        "Das Büro liegt im Hotel in Osnabrück.",
        "Wir wollen nicht widersprechen oder missverstehen.",
        "Die Lebensversicherung läuft im August aus.",
        "Sankt Martin ritt durch Schnee und Wind.",
        # Ziffern und Interpunktion roh (Textregeln kommen erst in M3 dazu).
        "Es sind 22 Grad und 0,1 Prozent.",
        # Frage und Ausruf.
        "Kommst du morgen? Ja, natürlich!",
        # Längerer Satz mit Einschub.
        "Der Mann, der dort am Fenster steht und wartet, ist mein Vater.",
        # Auslautverhärtung und Komposita.
        "Das Handtuch liegt im Studentenwohnheim neben der Kaffeemaschine.",
    ],
    "en-us": [
        "The meeting is on the third of August at eight o'clock.",
        "Water, butter, a little bottle.",
        "Through rough thorough thought, though.",
        "I'd like a glass of water, please.",
        "Is that right? Absolutely!",
    ],
    "en-gb": [
        "The meeting is on the third of August at eight o'clock.",
        "Water, butter, a little bottle.",
        "Rather a bath in the castle.",
        "I'd like a glass of water, please.",
    ],
    "es": [
        "La reunión es el tres de agosto a las ocho.",
        "El veloz murciélago hindú comía feliz cardillo y kiwi.",
        "¿Cómo estás? ¡Muy bien!",
        "Cinco perros grandes gruñían en la niebla.",
    ],
    "fr-fr": [
        "Le rendez-vous est le trois août à huit heures.",
        "Portez ce vieux whisky au juge blond qui fume.",
        "Aujourd'hui, c'est l'été.",
        "Où est l'hôtel, s'il vous plaît?",
    ],
    "it": [
        "L'incontro è il tre agosto alle otto.",
        "Gli gnocchi con la sciarpa e il ghiaccio.",
        "Perché già più così?",
        "Cinque chiacchiere in piazza.",
    ],
    "pt-br": [
        "A reunião é no dia três de agosto às oito horas.",
        "Não, então, coração, mãe e irmã.",
        "Zebras caolhas de Java querem passar fax para moças gigantes.",
        "A chuva molhou a palha do ninho.",
    ],
    "hi": [
        "आज तीन अगस्त है। बैठक आठ बजे शुरू होगी।",
        "नमस्ते, आप कैसे हैं?",
        "पानी बहुत ठंडा है।",
    ],
}
