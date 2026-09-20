#!/usr/bin/env python3
"""Train TextBlock's tiny, reproducible political SMS neural classifier."""

from __future__ import annotations

import argparse
import math
import random
import re
import struct
import unicodedata
from pathlib import Path

import numpy as np


SEED = 7242010
INPUT_SIZE = 2048
HIDDEN_SIZE = 16
DEFAULT_THRESHOLD = 0.93

NAMES = ["Alex", "Barack Obama", "Donald Trump", "Maria", "Sen. Brooks", "Gov. Lee"]
PARTIES = ["Democrats", "Republicans", "the Democratic Party", "the GOP"]
OFFICES = ["Congress", "Senate", "county council", "school board", "House"]
LINKS = [
    "https://fwd-blue.com/l/abc",
    "https://secure.actblue.com/donate/x",
    "https://winred.com/xyz",
    "https://takeit-back.org/a",
]
FOOTERS = ["Reply STOP to end", "Stop2Quit", "Stop to End", "Paid for by Citizens PAC"]

POLITICAL_TEMPLATES = [
    "{name} needs your help to win {office}. Can you chip in $15? {link}",
    "Breaking: {name} just made a huge announcement. Read more now {link}",
    "Kyle, your input will shape {party} strategy. Take our one minute survey {link}",
    "Will you vote for {name} in the primary election? Reply YES or NO",
    "We are one seat away from taking back the {office}. Donate before midnight {link}",
    "Election day is tomorrow. Make a plan to vote for {name}",
    "Urgent petition: tell Congress to protect voting rights. Add your name {link}",
    "{party} are counting on grassroots donors like you. Rush $7 now {link}",
    "Our polling shows this race is a tossup. Help {name} fight back {link}",
    "Barack Obama met with House Democrats and made an amazing statement {link}",
    "Do you approve of the president? Complete our national voter poll {link}",
    "The radical agenda must be stopped. Support our candidates today {link}",
    "We missed our fundraising deadline. Can you contribute before 11:59 PM? {link}",
    "Your district could decide the House majority. Sign up to canvass {link}",
    "Final notice for Maryland voters: early voting begins Friday {link}",
    "Endorse {name} for {office}. Sign the petition now {link}",
    "MATCH ALERT: every donation is triple matched for the next 24 hours {link}",
    "This is {name}'s campaign. We need 43 more donations from your area {link}",
    "Political update from ForwardBlue: read the latest statement {link}",
    "Can {party} count on your vote in November? Respond now",
    "Survey: should Congress pass this bill? Your response is needed {link}",
    "A new attack ad just dropped against {name}. Watch and donate {link}",
    "We are building a grassroots campaign for {office}. Join us {link}",
    "The primary is days away and we are behind. Pitch in $25 {link}",
    "Paid for by Americans for {name}. Not authorized by any candidate. {link}",
    "Your voter registration deadline is approaching. Learn more {link}",
    "Help flip this seat blue and elect {name} to {office} {link}",
    "Help defend this seat and keep {party} in the majority {link}",
    "Candidate town hall tonight. RSVP to hear from {name} {link}",
    "We need signatures to get {name} on the ballot. Add yours {link}",
]

NON_POLITICAL_TEMPLATES = [
    "Mariah: Can you pick up oat milk on your way home?",
    "Your pharmacy order is ready for pickup. Reply STOP to end reminders",
    "Chase alert: your external transfer of $358.72 was completed",
    "Your grocery delivery has arrived. View order details {commerce_link}",
    "Dentistry for Children: your appointment is tomorrow at 9:30 AM",
    "Your verification code is 493747. Do not share this code",
    "School is closed tomorrow because of inclement weather",
    "Vote for the restaurant for Friday's team lunch in the office poll",
    "Congress Avenue is closed due to construction. Use another route",
    "Your donation receipt from the animal shelter is attached",
    "The board meeting starts at 7 PM in the community room",
    "Flight 724 is delayed. Check the airline app for updates",
    "Package delivered at the front door. Track it here {commerce_link}",
    "Your bank statement is available. Sign in through the official app",
    "Reminder: Claire has a pediatrician appointment Thursday",
    "Hey, this is Laura. We made it home safely",
    "Security alert: a new device signed into your account",
    "Your table is confirmed for 6:30 PM. Reply C to cancel",
    "Library notice: your book is due in three days",
    "Weather alert: severe thunderstorms are expected in your area",
    "The plumber will arrive between 10 AM and noon",
    "Your one-time passcode is 218944",
    "Thanks for your purchase. Your receipt total is $25.00",
    "The neighborhood association election results are posted in the portal",
    "I watched the debate last night and thought it was interesting",
    "Can you send me the picture from Danny's school event?",
    "Your doctor sent a new message in the patient portal",
    "Payment due reminder for your electric bill",
    "Emergency maintenance will test the fire alarm tomorrow",
    "Your order is out for delivery. Text STOP to stop shipping alerts",
    "We value your feedback. Take a survey about your recent store visit",
    "The museum newsletter is ready. Unsubscribe using the link below",
    "Your employer benefits enrollment deadline is Friday",
    "NASA campus notice: the building will close early today",
    "Your aquarium filter subscription ships tomorrow",
    "Teddy's veterinary appointment is confirmed for Monday",
    "The soccer team needs volunteers for Saturday's game",
    "Your password was changed successfully",
    "Movie night poll: vote for Dune or Arrival",
    "I disagree with that senator, but this is just me venting",
    "County recycling pickup is delayed one day this week",
    "Your credit card payment was received",
    "The contractor sent an updated estimate for the roof repair",
    "Dinner is ready whenever you get home",
    "Your prescription refill request was approved",
    "Reminder from the daycare: bring extra clothes tomorrow",
    "The concert starts at 8 PM. Your mobile tickets are ready",
    "A login attempt was blocked. Review your account security",
    "Your internet service appointment is scheduled for Wednesday",
    "Thanks for joining our rewards program. Reply STOP to end offers",
]


def fill(template: str, rng: random.Random) -> str:
    return template.format(
        name=rng.choice(NAMES),
        party=rng.choice(PARTIES),
        office=rng.choice(OFFICES),
        link=rng.choice(LINKS),
        commerce_link=rng.choice(["https://ups.com/t/x", "https://store.example/order/7"]),
    )


def mutate(text: str, rng: random.Random, political: bool) -> str:
    if political and rng.random() < 0.72:
        text = f"{text}\n\n{rng.choice(FOOTERS)}"
    if rng.random() < 0.20:
        text = text.upper()
    elif rng.random() < 0.30:
        text = text.lower()
    if rng.random() < 0.35:
        text = text.replace(" ", rng.choice(["  ", " ", "\n"]))
    if rng.random() < 0.25:
        text = rng.choice(["ALERT: ", "Hi Kyle, ", "Update: "]) + text
    return text


def fnv1a(value: str) -> int:
    result = 0x811C9DC5
    for byte in value.encode("utf-8"):
        result ^= byte
        result = (result * 0x01000193) & 0xFFFFFFFF
    return result


def normalize(text: str) -> str:
    return " ".join(unicodedata.normalize("NFKC", text).lower().split())[:600]


def vectorize(text: str) -> np.ndarray:
    text = normalize(text)
    counts: dict[int, float] = {}
    words = re.findall(r"[a-z0-9$]+", text)
    features = [f"w:{word}" for word in words]
    features.extend(f"b:{a}_{b}" for a, b in zip(words, words[1:]))
    padded = f"  {text}  "
    for size in (3, 4, 5):
        features.extend(f"c:{padded[i:i + size]}" for i in range(len(padded) - size + 1))
    for feature in features:
        hashed = fnv1a(feature)
        index = hashed & (INPUT_SIZE - 1)
        sign = -1.0 if hashed & 0x80000000 else 1.0
        counts[index] = counts.get(index, 0.0) + sign
    vector = np.zeros(INPUT_SIZE, dtype=np.float32)
    for index, value in counts.items():
        vector[index] = value
    norm = float(np.linalg.norm(vector))
    return vector / norm if norm > 0 else vector


def dataset(templates: list[str], label: int, rng: random.Random, count_each: int) -> tuple[np.ndarray, np.ndarray]:
    texts = [mutate(fill(template, rng), rng, label == 1) for template in templates for _ in range(count_each)]
    return np.stack([vectorize(text) for text in texts]), np.full(len(texts), label, dtype=np.float32)


def sigmoid(values: np.ndarray) -> np.ndarray:
    return 1.0 / (1.0 + np.exp(-np.clip(values, -30.0, 30.0)))


def train(x: np.ndarray, y: np.ndarray) -> tuple[np.ndarray, np.ndarray, np.ndarray, np.float32]:
    rng = np.random.default_rng(SEED)
    w1 = rng.normal(0.0, 0.025, (HIDDEN_SIZE, INPUT_SIZE)).astype(np.float32)
    b1 = np.zeros(HIDDEN_SIZE, dtype=np.float32)
    w2 = rng.normal(0.0, 0.08, HIDDEN_SIZE).astype(np.float32)
    b2 = np.float32(0.0)
    params = [w1, b1, w2]
    first = [np.zeros_like(param) for param in params]
    second = [np.zeros_like(param) for param in params]
    first_b2 = 0.0
    second_b2 = 0.0
    step = 0
    batch_size = 96
    for epoch in range(180):
        order = rng.permutation(len(x))
        for start in range(0, len(x), batch_size):
            step += 1
            batch = order[start:start + batch_size]
            xb, yb = x[batch], y[batch]
            hidden = np.tanh(xb @ w1.T + b1)
            probability = sigmoid(hidden @ w2 + b2)
            output_delta = (probability - yb) / len(batch)
            gradients = [
                ((output_delta[:, None] * w2) * (1.0 - hidden * hidden)).T @ xb + 2e-5 * w1,
                np.sum((output_delta[:, None] * w2) * (1.0 - hidden * hidden), axis=0),
                hidden.T @ output_delta + 2e-5 * w2,
            ]
            gradient_b2 = float(np.sum(output_delta))
            for index, (param, gradient) in enumerate(zip(params, gradients)):
                first[index] = 0.9 * first[index] + 0.1 * gradient
                second[index] = 0.999 * second[index] + 0.001 * gradient * gradient
                corrected_first = first[index] / (1.0 - 0.9**step)
                corrected_second = second[index] / (1.0 - 0.999**step)
                param -= 0.003 * corrected_first / (np.sqrt(corrected_second) + 1e-8)
            first_b2 = 0.9 * first_b2 + 0.1 * gradient_b2
            second_b2 = 0.999 * second_b2 + 0.001 * gradient_b2 * gradient_b2
            b2 -= np.float32(0.003 * (first_b2 / (1.0 - 0.9**step)) / (math.sqrt(second_b2 / (1.0 - 0.999**step)) + 1e-8))
    return w1, b1, w2, b2


def predict(x: np.ndarray, model: tuple[np.ndarray, np.ndarray, np.ndarray, np.float32]) -> np.ndarray:
    w1, b1, w2, b2 = model
    return sigmoid(np.tanh(x @ w1.T + b1) @ w2 + b2)


def write_model(path: Path, model: tuple[np.ndarray, np.ndarray, np.ndarray, np.float32]) -> None:
    w1, b1, w2, b2 = model
    path.parent.mkdir(parents=True, exist_ok=True)
    with path.open("wb") as output:
        output.write(b"TBNN")
        output.write(struct.pack(">iiif", 1, INPUT_SIZE, HIDDEN_SIZE, DEFAULT_THRESHOLD))
        for values in (w1.reshape(-1), b1, w2, np.asarray([b2], dtype=np.float32)):
            output.write(np.asarray(values, dtype=">f4").tobytes())


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    rng = random.Random(SEED)
    positive_split = 24
    negative_split = 40
    train_pos = dataset(POLITICAL_TEMPLATES[:positive_split], 1, rng, 22)
    train_neg = dataset(NON_POLITICAL_TEMPLATES[:negative_split], 0, rng, 14)
    test_pos = dataset(POLITICAL_TEMPLATES[positive_split:], 1, rng, 12)
    test_neg = dataset(NON_POLITICAL_TEMPLATES[negative_split:], 0, rng, 12)
    x_train = np.concatenate([train_pos[0], train_neg[0]])
    y_train = np.concatenate([train_pos[1], train_neg[1]])
    model = train(x_train, y_train)
    x_test = np.concatenate([test_pos[0], test_neg[0]])
    y_test = np.concatenate([test_pos[1], test_neg[1]])
    scores = predict(x_test, model)
    predicted = scores >= DEFAULT_THRESHOLD
    false_positive = int(np.sum(predicted & (y_test == 0)))
    false_negative = int(np.sum(~predicted & (y_test == 1)))
    print(f"held-out examples={len(y_test)} false_positive={false_positive} false_negative={false_negative}")
    print(f"negative max={scores[y_test == 0].max():.4f} positive min={scores[y_test == 1].min():.4f}")
    write_model(args.output, model)
    print(f"wrote {args.output} ({args.output.stat().st_size} bytes)")


if __name__ == "__main__":
    main()
