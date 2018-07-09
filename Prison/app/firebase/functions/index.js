const functions = require('firebase-functions');
const nodemailer = require('nodemailer');

const gmailEmail = functions.config().gmail.email;
const gmailPassword = functions.config().gmail.password;
const mailTransport = nodemailer.createTransport({
  service: 'gmail',
  auth: {
    user: gmailEmail,
    pass: gmailPassword,
  },
});

exports.sendWelcomeEmail = functions.auth.user().onCreate((user) => {
    const email = user.email;

    const mailOptions = {
        from: "Prison App <noreply@firebase.com>",
        to: email,
        subject: "Welcome to Prison!",
        text: "Enjoy the stay //The Guards"
    }

    console.log("trying to send: ", mailOptions)

    if (email === null){
        return console.log("No welcome email was sent to anonymous login")
    } else {
        return mailTransport.sendMail(mailOptions).then(()=>{
            return console.log("Welcome email was sent to: ", email)
        })
    }
});